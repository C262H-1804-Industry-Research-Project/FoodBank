package foodbank.webapp.api;

import com.jayway.jsonpath.JsonPath;
import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.repositories.AccountLinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountLinkApiTest extends ApiTestSupport {
    @Autowired AccountLinkRepository accountLinkRepository;

    private ResultActions link(String token, Integer targetId, String role, String password) throws Exception {
        String email = userRepository.findById(targetId).orElseThrow().getEmail();
        return mockMvc.perform(post("/api/account/link")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetRole":"%s","email":"%s","password":"%s"}
                        """.formatted(role, email, password)));
    }

    private String switchTo(String token, String role) throws Exception {
        String body = mockMvc.perform(post("/api/account/switch-role")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetRole\":\"" + role + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value(role))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void linkAndSwitchBothWaysRetainsAccountIdsAndRevokesOldToken(boolean startAsDonor) throws Exception {
        Donor donor = createDonor();
        Recipient recipient = createRecipient();
        Integer sourceId = startAsDonor ? donor.getUserId() : recipient.getUserId();
        Integer targetId = startAsDonor ? recipient.getUserId() : donor.getUserId();
        String sourceRole = startAsDonor ? "donor" : "recipient";
        String targetRole = startAsDonor ? "recipient" : "donor";
        String original = bearer(sourceId, sourceRole);
        link(original, targetId, targetRole, PASSWORD).andExpect(status().isOk());
        assertThat(accountLinkRepository.findByDonorUserId(donor.getUserId()).orElseThrow()
                .getRecipientUserId()).isEqualTo(recipient.getUserId());

        String switched = switchTo(original, targetRole);
        assertThat(authTokenService.getClaims(switched).getSubject()).isEqualTo(targetId.toString());
        assertThat(authTokenService.isActive(original.substring(7))).isFalse();
        String returned = switchTo("Bearer " + switched, sourceRole);
        assertThat(authTokenService.getClaims(returned).getSubject()).isEqualTo(sourceId.toString());
        if (sourceRole.equals("donor")) {
            mockMvc.perform(get("/api/requests").header("Authorization", "Bearer " + returned))
                    .andExpect(status().isForbidden());
        }
        // A fresh login still uses the stored link, without re-entering the other password.
        String fresh = bearer(sourceId, sourceRole);
        switchTo(fresh, targetRole);
    }

    @Test
    void wrongPasswordNeverCreatesALink() throws Exception {
        Donor donor = createDonor();
        Recipient recipient = createRecipient();
        link(bearer(donor.getUserId(), "donor"), recipient.getUserId(), "recipient", "wrong-password")
                .andExpect(status().isUnauthorized());
        assertThat(accountLinkRepository.findByDonorUserId(donor.getUserId())).isEmpty();
    }

    @Test
    void sameEmailWithDifferentPasswordsIsSupported() throws Exception {
        Donor donor = createDonor();
        Recipient recipient = createRecipient();
        var recipientUser = userRepository.findById(recipient.getUserId()).orElseThrow();
        recipientUser.setEmail(userRepository.findById(donor.getUserId()).orElseThrow().getEmail());
        recipientUser.setPassword(passwordEncoder.encode("Different123!"));
        userRepository.save(recipientUser);
        link(bearer(donor.getUserId(), "donor"), recipient.getUserId(), "recipient", "Different123!")
                .andExpect(status().isOk());
    }

    @Test
    void cannotReplaceAnExistingLinkOrCreateACompetingProfile() throws Exception {
        Donor donor = createDonor();
        Donor other = createDonor();
        Recipient recipient = createRecipient();
        String token = bearer(donor.getUserId(), "donor");
        link(token, recipient.getUserId(), "recipient", PASSWORD).andExpect(status().isOk());
        link(bearer(other.getUserId(), "donor"), recipient.getUserId(), "recipient", PASSWORD)
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/account/add-role").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetRole\":\"recipient\",\"recipientName\":\"Other profile\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void bannedAndUnapprovedAccountsCannotBeLinked() throws Exception {
        Donor donor = createDonor();
        Recipient recipient = createRecipient();
        var user = userRepository.findById(recipient.getUserId()).orElseThrow();
        user.setStatus("BANNED");
        userRepository.save(user);
        String token = bearer(donor.getUserId(), "donor");
        link(token, recipient.getUserId(), "recipient", PASSWORD).andExpect(status().isForbidden());
        user.setStatus("ACTIVE");
        userRepository.save(user);
        recipient.setApprovalStatus("PENDING");
        recipientRepository.save(recipient);
        link(token, recipient.getUserId(), "recipient", PASSWORD).andExpect(status().isForbidden());
        assertThat(accountLinkRepository.findByDonorUserId(donor.getUserId())).isEmpty();
    }

    @Test
    void laterBanPreventsSwitchingAndLeavesCurrentTokenActive() throws Exception {
        Donor donor = createDonor();
        Recipient recipient = createRecipient();
        String token = bearer(donor.getUserId(), "donor");
        link(token, recipient.getUserId(), "recipient", PASSWORD).andExpect(status().isOk());
        var user = userRepository.findById(recipient.getUserId()).orElseThrow();
        user.setStatus("BANNED");
        userRepository.save(user);
        mockMvc.perform(post("/api/account/switch-role").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetRole\":\"recipient\"}"))
                .andExpect(status().isForbidden());
        assertThat(authTokenService.isActive(token.substring(7))).isTrue();
    }

    @Test
    void unauthenticatedAndAdminRequestsAreRejected() throws Exception {
        mockMvc.perform(post("/api/account/link").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/account/link").header("Authorization", bearer(123, "admin"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void existingDualRoleAccountCannotBeLinkedToAnotherAccount() throws Exception {
        Donor donor = createDonor();
        Recipient recipient = createRecipient();
        recipientRepository.save(Recipient.builder()
                .userId(donor.getUserId()).name("Existing second profile").build());
        link(bearer(donor.getUserId(), "donor"), recipient.getUserId(), "recipient", PASSWORD)
                .andExpect(status().isConflict());
        assertThat(accountLinkRepository.findByDonorUserId(donor.getUserId())).isEmpty();
    }

    @Test
    void bannedSourceCannotLinkEvenWithAnOldActiveToken() throws Exception {
        Donor donor = createDonor();
        Recipient recipient = createRecipient();
        String token = bearer(donor.getUserId(), "donor");
        var user = userRepository.findById(donor.getUserId()).orElseThrow();
        user.setStatus("BANNED");
        userRepository.save(user);
        link(token, recipient.getUserId(), "recipient", PASSWORD).andExpect(status().isForbidden());
        assertThat(accountLinkRepository.findByDonorUserId(donor.getUserId())).isEmpty();
    }

    @Test
    void linkingSameRoleIsRejected() throws Exception {
        Donor donor = createDonor();
        Donor other = createDonor();
        link(bearer(donor.getUserId(), "donor"), other.getUserId(), "donor", PASSWORD)
                .andExpect(status().isBadRequest());
    }
}
