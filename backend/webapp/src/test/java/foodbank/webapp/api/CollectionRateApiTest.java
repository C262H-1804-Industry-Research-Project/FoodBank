package foodbank.webapp.api;

import foodbank.webapp.entity.CollectionHistory;
import foodbank.webapp.entity.Request;
import foodbank.webapp.repositories.CollectionHistoryRepository;
import foodbank.webapp.repositories.RequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.contains;

@TestPropertySource(properties = "JWT_SECRET=dGVzdC1vbmx5LWp3dC1zZWNyZXQta2V5LWZvci1mb29kYmFuay0xMjM0NTY3OA==")
class CollectionRateApiTest extends ApiTestSupport {
    @Autowired private RequestRepository requestRepository;
    @Autowired private CollectionHistoryRepository historyRepository;

    private void assertAdminRate(Integer userId, Integer listingId, int completed, int approved) throws Exception {
        String row = "$.content[?(@.listing.listingId == " + listingId + ")]";
        mockMvc.perform(get("/api/admin/listings").param("size", "100")
                        .header("Authorization", bearer(userId, "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath(row + ".completedRequests", contains(completed)))
                .andExpect(jsonPath(row + ".approvedRequests", contains(approved)));
    }

    @Test
    void threeApprovedRequestsWithOneCompletedHistoryReturnOneOutOfThree() throws Exception {
        var donor = createDonor();
        var listing = createLiveListing(donor, "Milk");
        var slot = createTimeslot(listing, 5);
        var recipient = createRecipient();
        for (int i = 0; i < 3; i++) {
            var request = requestRepository.save(Request.builder().timeslotId(slot.getTimeslotId())
                    .recipientId(recipient.getRecipientId()).quantityRequested(5f).status("APPROVED").build());
            if (i == 0) {
                // Multiple history rows must not count the same completed application twice.
                for (int j = 0; j < 2; j++) {
                    historyRepository.save(CollectionHistory.builder().requestId(request.getRequestId())
                            .listingId(listing.getListingId()).status("COMPLETED").build());
                }
            }
        }
        mockMvc.perform(get("/api/donors/{donorId}/listings", donor.getId())
                        .header("Authorization", bearer(donor.getUserId(), "donor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].completedRequests").value(1))
                .andExpect(jsonPath("$[0].approvedRequests").value(3));
        assertAdminRate(donor.getUserId(), listing.getListingId(), 1, 3);
    }

    @Test
    void completedAndFailedRemainInDenominatorWhileOtherListingsAndUnapprovedAreExcluded() throws Exception {
        var donor = createDonor();
        var listing = createLiveListing(donor, "Milk");
        var slot = createTimeslot(listing, 5);
        var other = createTimeslot(createLiveListing(createDonor(), "Bread"), 2);
        for (String state : new String[]{"APPROVED", "COMPLETED", "FAILED", "PENDING", "REJECTED", "CANCELLED"}) {
            requestRepository.save(Request.builder().timeslotId(slot.getTimeslotId()).status(state).build());
            requestRepository.save(Request.builder().timeslotId(other.getTimeslotId()).status(state).build());
        }
        mockMvc.perform(get("/api/donors/{donorId}/listings", donor.getId())
                        .header("Authorization", bearer(donor.getUserId(), "donor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].completedRequests").value(1))
                .andExpect(jsonPath("$[0].approvedRequests").value(3));
        assertAdminRate(donor.getUserId(), listing.getListingId(), 1, 3);
    }

    @Test
    void listingWithoutRequestsReturnsZeroCounts() throws Exception {
        var donor = createDonor();
        var listing = createLiveListing(donor, "Milk");
        mockMvc.perform(get("/api/donors/{donorId}/listings", donor.getId())
                        .header("Authorization", bearer(donor.getUserId(), "donor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].completedRequests").value(0))
                .andExpect(jsonPath("$[0].approvedRequests").value(0));
        assertAdminRate(donor.getUserId(), listing.getListingId(), 0, 0);
    }
}
