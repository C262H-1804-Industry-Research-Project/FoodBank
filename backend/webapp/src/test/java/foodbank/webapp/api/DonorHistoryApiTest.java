package foodbank.webapp.api;

import foodbank.webapp.entity.CollectionHistory;
import foodbank.webapp.entity.Donor;
import foodbank.webapp.repositories.CollectionHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Keep local application credentials from overriding the test-only signing key.
@TestPropertySource(properties = "JWT_SECRET=dGVzdC1vbmx5LWp3dC1zZWNyZXQta2V5LWZvci1mb29kYmFuay0xMjM0NTY3OA==")
class DonorHistoryApiTest extends ApiTestSupport {
    @Autowired private CollectionHistoryRepository historyRepository;

    private void addHistory(Donor donor, LocalDateTime time) {
        var listing = createLiveListing(donor, "Carrot crate");
        var recipient = createRecipient();
        historyRepository.save(CollectionHistory.builder()
                .donorId(donor.getId()).listingId(listing.getListingId())
                .recipientId(recipient.getRecipientId()).category("Produce")
                .quantityCollected(3f).collectedDatetime(time).pickupLocation("Adelaide").build());
    }

    @Test
    void returnsOnlyOwnHistoryNewestFirstWithListingAndRecipient() throws Exception {
        var donor = createDonor();
        addHistory(donor, LocalDateTime.of(2026, 9, 1, 10, 0));
        addHistory(donor, LocalDateTime.of(2026, 9, 2, 10, 0));
        addHistory(createDonor(), LocalDateTime.of(2026, 9, 3, 10, 0));

        mockMvc.perform(get("/api/donors/{donorId}/collection-history", donor.getId())
                        .header("Authorization", bearer(donor.getUserId(), "donor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].foodName").value("Carrot crate"))
                .andExpect(jsonPath("$[0].recipientName").value("Test Recipient"))
                .andExpect(jsonPath("$[0].quantityCollected").value(3))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].collectedDatetime").value("2026-09-02T10:00"));
    }

    @Test
    void returnsFailedOutcomeAndHandlesMissingRelatedRecords() throws Exception {
        var donor = createDonor();
        historyRepository.save(CollectionHistory.builder()
                .donorId(donor.getId()).status("FAILED").quantityCollected(0f)
                .collectedDatetime(LocalDateTime.of(2026, 10, 16, 12, 0)).build());
        assertThat(historyRepository.countSuccessfulByDonorId(donor.getId())).isZero();
        mockMvc.perform(get("/api/donors/{donorId}/collection-history", donor.getId())
                        .header("Authorization", bearer(donor.getUserId(), "donor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[0].quantityCollected").value(0))
                .andExpect(jsonPath("$[0].foodName").isEmpty())
                .andExpect(jsonPath("$[0].recipientName").isEmpty());
    }

    @Test
    void emptyHistoryReturnsEmptyArray() throws Exception {
        var donor = createDonor();
        mockMvc.perform(get("/api/donors/{donorId}/collection-history", donor.getId())
                        .header("Authorization", bearer(donor.getUserId(), "donor")))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectsAnonymousRecipientAndOtherDonor() throws Exception {
        var donor = createDonor();
        var other = createDonor();
        mockMvc.perform(get("/api/donors/{donorId}/collection-history", donor.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/donors/{donorId}/collection-history", donor.getId())
                        .header("Authorization", recipientToken(createRecipient())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/donors/{donorId}/collection-history", donor.getId())
                        .header("Authorization", bearer(other.getUserId(), "donor")))
                .andExpect(status().isForbidden());
    }
}
