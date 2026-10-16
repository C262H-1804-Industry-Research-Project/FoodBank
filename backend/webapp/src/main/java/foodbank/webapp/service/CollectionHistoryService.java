package foodbank.webapp.service;

import foodbank.webapp.dto.CollectionHistoryResponse;
import foodbank.webapp.entity.CollectionHistory;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.repositories.CollectionHistoryRepository;
import foodbank.webapp.repositories.ListingRepository;
import foodbank.webapp.repositories.RecipientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectionHistoryService {
    private final CollectionHistoryRepository collectionHistoryRepository;
    private final RecipientRepository recipientRepository;
    private final ListingRepository listingRepository;

    @Transactional(readOnly = true)
    public List<CollectionHistoryResponse> getHistoryByDonor(Integer donorId) {
        return collectionHistoryRepository.findByDonorIdOrderByCollectedDatetimeDescHistoryIdDesc(donorId).stream()
                .map(this::toResponse)
                .toList();
    }

    private CollectionHistoryResponse toResponse(CollectionHistory history) {
        String recipientName = null;
        if (history.getRecipientId() != null) {
            recipientName = recipientRepository.findById(history.getRecipientId())
                    .map(Recipient::getName)
                    .orElse(null);
        }

        return new CollectionHistoryResponse(
                history.getHistoryId(),
                history.getCategory(),
                history.getQuantityCollected(),
                history.getCollectedDatetime() == null ? null : history.getCollectedDatetime().toString(),
                recipientName,
                history.getPickupLocation(),
                history.getListingId() == null ? null : listingRepository.findById(history.getListingId())
                        .map(Listing::getName).orElse(null),
                history.getStatus() == null || history.getStatus().isBlank()
                        ? "COMPLETED" : history.getStatus().toUpperCase(java.util.Locale.ROOT)
        );
    }
}
