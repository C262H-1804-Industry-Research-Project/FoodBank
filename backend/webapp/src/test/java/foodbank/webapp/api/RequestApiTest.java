package foodbank.webapp.api;

import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.entity.Timeslot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RequestApiTest extends ApiTestSupport {

    private Recipient recipient;
    private Donor donor;
    private Listing listing;
    private Timeslot timeslot;

    @BeforeEach
    void setUp() {
        recipient = createRecipient();
        donor = createDonor();
        listing = createLiveListing(donor, "Bread rolls");
        timeslot = createTimeslot(listing, 3);
    }

    private ResultActions submit(String token, String body) throws Exception {
        var request = post("/api/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (token != null) {
            request.header("Authorization", token);
        }
        return mockMvc.perform(request);
    }

    private String body(Integer timeslotId, Number quantity, String noteJson) {
        return """
                {"timeslotId": %s, "quantityRequested": %s, "note": %s}
                """.formatted(timeslotId, quantity, noteJson);
    }

    @Test
    void createsPendingRequestForSignedInRecipient() throws Exception {
        submit(recipientToken(recipient), body(timeslot.getTimeslotId(), 2, "\"  Will arrive at 10:15  \""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.recipientName").value("Test Recipient"))
                .andExpect(jsonPath("$.listingName").value("Bread rolls"))
                .andExpect(jsonPath("$.quantityRequested").value(2.0))
                .andExpect(jsonPath("$.note").value("Will arrive at 10:15"))
                .andExpect(jsonPath("$.pickupCode").value(nullValue()));

        assertThat(timeslotRepository.findById(timeslot.getTimeslotId()).orElseThrow().getRemainingSlot())
                .isEqualTo(2);
    }

    @Test
    void blankNoteIsStoredAsNull() throws Exception {
        submit(recipientToken(recipient), body(timeslot.getTimeslotId(), 1, "\"   \""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.note").value(nullValue()));
    }

    @Test
    void autoApprovedListingIssuesPickupCode() throws Exception {
        listing.setIsAutoApproved(true);
        listing.setUsePickupCode(true);
        listingRepository.save(listing);

        submit(recipientToken(recipient), body(timeslot.getTimeslotId(), 1, "null"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.pickupCode").value(matchesPattern("\\d{6}")));
    }

    @Test
    void rejectsMissingToken() throws Exception {
        submit(null, body(timeslot.getTimeslotId(), 1, "null"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDonorToken() throws Exception {
        submit(bearer(donor.getUserId(), "donor"), body(timeslot.getTimeslotId(), 1, "null"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsNoteOver500Characters() throws Exception {
        submit(recipientToken(recipient), body(timeslot.getTimeslotId(), 1, "\"" + "a".repeat(501) + "\""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNonPositiveQuantity() throws Exception {
        submit(recipientToken(recipient), body(timeslot.getTimeslotId(), 0, "null"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnknownTimeslot() throws Exception {
        submit(recipientToken(recipient), body(999_999, 1, "null"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsFullyBookedTimeslot() throws Exception {
        Timeslot full = createTimeslot(listing, 0);
        submit(recipientToken(recipient), body(full.getTimeslotId(), 1, "null"))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsDuplicateRequestForSameTimeslot() throws Exception {
        String token = recipientToken(recipient);
        submit(token, body(timeslot.getTimeslotId(), 1, "null")).andExpect(status().isCreated());
        submit(token, body(timeslot.getTimeslotId(), 1, "null")).andExpect(status().isConflict());
    }

    @Test
    void rejectsListingThatIsNotLive() throws Exception {
        listing.setStatus("DRAFT");
        listingRepository.save(listing);

        submit(recipientToken(recipient), body(timeslot.getTimeslotId(), 1, "null"))
                .andExpect(status().isConflict());
    }

    @Test
    void myRequestsOnlyReturnsSignedInRecipientsRequests() throws Exception {
        Recipient other = createRecipient();
        submit(recipientToken(recipient), body(timeslot.getTimeslotId(), 1, "\"mine\""))
                .andExpect(status().isCreated());
        submit(recipientToken(other), body(timeslot.getTimeslotId(), 1, "\"theirs\""))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/requests").header("Authorization", recipientToken(recipient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].note").value("mine"));
    }
}
