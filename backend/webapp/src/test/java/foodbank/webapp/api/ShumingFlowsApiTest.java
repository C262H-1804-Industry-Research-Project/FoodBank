package foodbank.webapp.api;

import foodbank.webapp.entity.Admin;
import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.entity.Timeslot;
import foodbank.webapp.repositories.AdminRepository;
import foodbank.webapp.repositories.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ShumingFlowsApiTest extends ApiTestSupport {
    @Autowired RequestRepository requestRepository;
    @Autowired AdminRepository adminRepository;
    private Donor donor;
    private Recipient recipient;
    private Listing listing;
    private Timeslot slot;

    @BeforeEach
    void prepare() {
        donor = createDonor();
        recipient = createRecipient();
        recipient.setOrgName("Test Kitchen");
        recipientRepository.save(recipient);
        listing = createLiveListing(donor, "Test Food");
        listing.setUsePickupCode(true);
        listing.setAddressLine("50 Pickup Road");
        listingRepository.save(listing);
        slot = createTimeslot(listing, 2);
    }

    private String path(Donor owner) { return "/api/donors/" + owner.getId() + "/requests"; }
    private String donorToken() { return bearer(donor.getUserId(), "donor"); }
    private ResultActions submit(Recipient owner) throws Exception {
        return mockMvc.perform(post("/api/requests").header("Authorization", recipientToken(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timeslotId\":" + slot.getTimeslotId() + ",\"quantityRequested\":2,\"note\":\"  lunch  \"}"));
    }
    private int savedRequest() throws Exception {
        submit(recipient).andExpect(status().isCreated());
        return requestRepository.findByTimeslotId(slot.getTimeslotId()).get(0).getRequestId();
    }

    @Test
    void submitApproveViewCancelAndSubmitAgain() throws Exception {
        int id = savedRequest();
        mockMvc.perform(get("/api/requests").header("Authorization", recipientToken(recipient)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].pickupAddress").value(nullValue()))
                .andExpect(jsonPath("$[0].pickupCode").value(nullValue()))
                .andExpect(jsonPath("$[0].donorName").value("Test Donor"));
        mockMvc.perform(get(path(donor)).header("Authorization", donorToken()))
                .andExpect(jsonPath("$[0].recipientOrg").value("Test Kitchen"))
                .andExpect(jsonPath("$[0].recipientPhone").value("0400000000"))
                .andExpect(jsonPath("$[0].listingQuantity").value(10.0))
                .andExpect(jsonPath("$[0].note").value("lunch"));
        mockMvc.perform(patch(path(donor) + "/" + id + "/approve").header("Authorization", donorToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.pickupCode").value(matchesPattern("\\d{6}")));
        mockMvc.perform(get(path(donor)).header("Authorization", donorToken()))
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].status").value("APPROVED"));
        mockMvc.perform(get("/api/requests").header("Authorization", recipientToken(recipient)))
                .andExpect(jsonPath("$[0].pickupAddress").value("50 Pickup Road, Adelaide, SA"));
        mockMvc.perform(delete("/api/requests/" + id).header("Authorization", recipientToken(recipient)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/requests").header("Authorization", recipientToken(recipient)))
                .andExpect(jsonPath("$[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$[0].pickupCode").value(nullValue()))
                .andExpect(jsonPath("$[0].pickupAddress").value(nullValue()));
        assertThat(timeslotRepository.findById(slot.getTimeslotId()).orElseThrow().getRemainingSlot()).isEqualTo(2);
        submit(recipient).andExpect(status().isCreated());
    }

    @Test
    void rejectRestoresOnePlaceAndCannotBeRepeatedOrApproved() throws Exception {
        int id = savedRequest();
        mockMvc.perform(patch(path(donor) + "/" + id + "/reject").header("Authorization", donorToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        for (String action : new String[]{"approve", "reject"}) {
            mockMvc.perform(patch(path(donor) + "/" + id + "/" + action).header("Authorization", donorToken()))
                    .andExpect(status().isConflict());
        }
        assertThat(timeslotRepository.findById(slot.getTimeslotId()).orElseThrow().getRemainingSlot()).isEqualTo(2);
        mockMvc.perform(get(path(donor)).header("Authorization", donorToken()))
                .andExpect(jsonPath("$[0].status").value("REJECTED"));
    }

    @Test
    void anotherDonorCannotReadOrProcessThisRequest() throws Exception {
        int id = savedRequest();
        Donor other = createDonor();
        String token = bearer(other.getUserId(), "donor");
        mockMvc.perform(get(path(donor)).header("Authorization", token)).andExpect(status().isForbidden());
        for (String action : new String[]{"approve", "reject"}) {
            mockMvc.perform(patch(path(donor) + "/" + id + "/" + action).header("Authorization", token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(patch(path(other) + "/" + id + "/" + action).header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get(path(other)).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void recipientCannotSeeOrCancelAnotherRecipientsRequest() throws Exception {
        int id = savedRequest();
        Recipient other = createRecipient();
        mockMvc.perform(get("/api/requests").header("Authorization", recipientToken(other)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(delete("/api/requests/" + id).header("Authorization", recipientToken(other)))
                .andExpect(status().isForbidden());
    }

    @Test
    void pendingCancellationRestoresCapacityAndPreventsLaterProcessing() throws Exception {
        int id = savedRequest();
        mockMvc.perform(delete("/api/requests/" + id).header("Authorization", recipientToken(recipient)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/requests/" + id).header("Authorization", recipientToken(recipient)))
                .andExpect(status().isConflict());
        mockMvc.perform(patch(path(donor) + "/" + id + "/approve").header("Authorization", donorToken()))
                .andExpect(status().isConflict());
        assertThat(timeslotRepository.findById(slot.getTimeslotId()).orElseThrow().getRemainingSlot()).isEqualTo(2);
    }

    @Test
    void pickupStartPreventsCancellationAndNewSubmission() throws Exception {
        int id = savedRequest();
        slot.setStartTime(LocalDateTime.now().minusMinutes(1));
        timeslotRepository.save(slot);
        mockMvc.perform(delete("/api/requests/" + id).header("Authorization", recipientToken(recipient)))
                .andExpect(status().isConflict());
        submit(createRecipient()).andExpect(status().isConflict());
    }

    @Test
    void autoApprovalImmediatelyReturnsAddressAndCode() throws Exception {
        listing.setIsAutoApproved(true);
        listingRepository.save(listing);
        submit(recipient).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.pickupAddress").value("50 Pickup Road, Adelaide, SA"))
                .andExpect(jsonPath("$.pickupCode").value(matchesPattern("\\d{6}")));
    }

    @Test
    void donorEndpointsRequireDonorAuthentication() throws Exception {
        int id = savedRequest();
        mockMvc.perform(get(path(donor))).andExpect(status().isUnauthorized());
        mockMvc.perform(get(path(donor)).header("Authorization", recipientToken(recipient)))
                .andExpect(status().isForbidden());
        for (String action : new String[]{"approve", "reject"}) {
            mockMvc.perform(patch(path(donor) + "/" + id + "/" + action)
                            .header("Authorization", recipientToken(recipient)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void adminLoginAuthenticatesThePasswordAndProducesAWorkingAdminToken() throws Exception {
        String email = "staff-" + UUID.randomUUID() + "@example.com";
        adminRepository.save(Admin.builder().email(email).name("Test Staff")
                .password(passwordEncoder.encode(PASSWORD)).build());
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"admin\"}";
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("admin"))
                .andReturn().getResponse().getContentAsString();
        var match = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        assertThat(match.find()).isTrue();
        mockMvc.perform(get("/api/admin/reports").header("Authorization", "Bearer " + match.group(1)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace(PASSWORD, "incorrect"))).andExpect(status().isUnauthorized());
    }
}
