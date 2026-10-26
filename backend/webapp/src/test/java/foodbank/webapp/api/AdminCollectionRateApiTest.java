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

@TestPropertySource(properties = "JWT_SECRET=dGVzdC1vbmx5LWp3dC1zZWNyZXQta2V5LWZvci1mb29kYmFuay0xMjM0NTY3OA==")
class AdminCollectionRateApiTest extends ApiTestSupport {
    @Autowired private RequestRepository requestRepository;
    @Autowired private CollectionHistoryRepository historyRepository;

    private void assertRate(Integer userId, String role, int completed, int total) throws Exception {
        var user = userRepository.findById(userId).orElseThrow();
        mockMvc.perform(get("/api/admin/accounts").param("role", role).param("q", user.getEmail())
                        .header("Authorization", bearer(userId, "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(userId))
                .andExpect(jsonPath("$.content[0].completedCollections").value(completed))
                .andExpect(jsonPath("$.content[0].totalRequests").value(total));
    }

    @Test
    void countsAllRequestsAndActualCollectionsForEachRoleWithoutDuplicateHistory() throws Exception {
        var donor = createDonor();
        var recipient = createRecipient();
        var listing = createLiveListing(donor, "Milk");
        var slot = createTimeslot(listing, 10);
        for (String state : new String[]{"APPROVED", "COMPLETED", "FAILED", "PENDING", "REJECTED", "CANCELLED"}) {
            var request = requestRepository.save(Request.builder().timeslotId(slot.getTimeslotId())
                    .recipientId(recipient.getRecipientId()).status(state).build());
            if (state.equals("APPROVED")) {
                for (int i = 0; i < 2; i++) {
                    historyRepository.save(CollectionHistory.builder().requestId(request.getRequestId())
                            .listingId(listing.getListingId()).donorId(donor.getId())
                            .recipientId(recipient.getRecipientId()).status("COMPLETED").build());
                }
            }
            if (state.equals("FAILED")) {
                historyRepository.save(CollectionHistory.builder().requestId(request.getRequestId())
                        .listingId(listing.getListingId()).donorId(donor.getId())
                        .recipientId(recipient.getRecipientId()).status("FAILED").build());
            }
        }
        var otherSlot = createTimeslot(createLiveListing(createDonor(), "Bread"), 1);
        requestRepository.save(Request.builder().timeslotId(otherSlot.getTimeslotId())
                .recipientId(recipient.getRecipientId()).status("COMPLETED").build());
        assertRate(donor.getUserId(), "DONOR", 2, 6);
        assertRate(recipient.getUserId(), "RECIPIENT", 3, 7);
        assertRate(createRecipient().getUserId(), "RECIPIENT", 0, 0);
        assertRate(createDonor().getUserId(), "DONOR", 0, 0);
    }
}
