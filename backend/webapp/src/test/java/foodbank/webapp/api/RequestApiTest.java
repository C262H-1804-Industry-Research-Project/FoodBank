package foodbank.webapp.api;

import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.entity.Request;
import foodbank.webapp.entity.Timeslot;
import foodbank.webapp.repositories.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RequestApiTest extends ApiTestSupport {

    @Autowired private RequestRepository requestRepository;

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

    private Request saveRequest(Recipient owner, Timeslot slot, String status) {
        return requestRepository.save(Request.builder()
                .timeslotId(slot.getTimeslotId())
                .recipientId(owner.getRecipientId())
                .quantityRequested(1f)
                .status(status)
                .pickupCode("APPROVED".equals(status) ? "123456" : null)
                .build());
    }

    private ResultActions cancel(Request request, Recipient as) throws Exception {
        return mockMvc.perform(delete("/api/requests/{id}", request.getRequestId())
                .header("Authorization", recipientToken(as)));
    }

    @Test
    void cancelsFuturePendingAndApprovedRequestsAndFreesThePlace() throws Exception {
        for (String status : new String[] {"PENDING", "APPROVED"}) {
            int before = timeslotRepository.findById(timeslot.getTimeslotId()).orElseThrow().getRemainingSlot();
            Request request = saveRequest(recipient, timeslot, status);

            cancel(request, recipient).andExpect(status().isNoContent());

            Request saved = requestRepository.findById(request.getRequestId()).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo("CANCELLED");
            assertThat(saved.getPickupCode()).isNull();
            assertThat(timeslotRepository.findById(timeslot.getTimeslotId()).orElseThrow().getRemainingSlot())
                    .isEqualTo(before + 1);
        }
    }

    @Test
    void cannotCancelOnceThePickupHasStarted() throws Exception {
        Timeslot started = createTimeslot(listing, 3);
        started.setStartTime(java.time.LocalDateTime.now().minusMinutes(5));
        started.setEndTime(java.time.LocalDateTime.now().plusMinutes(55));
        timeslotRepository.save(started);
        Request request = saveRequest(recipient, started, "APPROVED");

        cancel(request, recipient).andExpect(status().isConflict());
        assertThat(requestRepository.findById(request.getRequestId()).orElseThrow().getStatus()).isEqualTo("APPROVED");
    }

    @Test
    void cannotCancelDeclinedOrAlreadyCancelledRequests() throws Exception {
        for (String status : new String[] {"REJECTED", "CANCELLED"}) {
            cancel(saveRequest(recipient, timeslot, status), recipient).andExpect(status().isConflict());
        }
    }

    @Test
    void cannotCancelAnotherRecipientsOrAnUnknownRequest() throws Exception {
        Request request = saveRequest(recipient, timeslot, "PENDING");

        cancel(request, createRecipient()).andExpect(status().isForbidden());
        assertThat(requestRepository.findById(request.getRequestId()).orElseThrow().getStatus()).isEqualTo("PENDING");

        mockMvc.perform(delete("/api/requests/{id}", 999999).header("Authorization", recipientToken(recipient)))
                .andExpect(status().isNotFound());
    }
}
