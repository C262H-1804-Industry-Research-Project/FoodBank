package foodbank.webapp.api;

import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Recipient;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountRoleApiTest extends ApiTestSupport {

    @Test
    void donorCanCreateRecipientProfileAndSwitchRole() throws Exception {
        Donor donor = createDonor();
        String token = bearer(donor.getUserId(), "donor");

        mockMvc.perform(post("/api/account/add-role")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetRole": "recipient",
                                  "recipientName": "New Recipient",
                                  "recipientOrgName": "Community Centre"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("recipient"));

        assertThat(
                recipientRepository.existsByUserId(donor.getUserId())
        ).isTrue();

        mockMvc.perform(post("/api/account/switch-role")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetRole": "recipient"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("recipient"))
                .andExpect(jsonPath("$.user.name").value("New Recipient"));
    }

    @Test
    void recipientCanCreateDonorProfileAndSwitchRole() throws Exception {
        Recipient recipient = createRecipient();
        String token = bearer(recipient.getUserId(), "recipient");

        mockMvc.perform(post("/api/account/add-role")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetRole": "donor",
                                  "donorOrgName": "New Food Store",
                                  "addressLine": "20 Test Street",
                                  "suburb": "Adelaide",
                                  "region": "SA"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("donor"));

        assertThat(
                donorRepository.existsByUserId(recipient.getUserId())
        ).isTrue();

        mockMvc.perform(post("/api/account/switch-role")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetRole": "donor"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("donor"))
                .andExpect(jsonPath("$.user.name").value("New Food Store"));
    }
}
