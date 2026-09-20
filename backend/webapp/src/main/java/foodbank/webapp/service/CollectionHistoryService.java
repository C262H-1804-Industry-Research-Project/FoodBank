package foodbank.webapp.service;

import foodbank.webapp.dto.CollectionHistoryResponse;
import foodbank.webapp.entity.CollectionHistory;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.repositories.CollectionHistoryRepository;
import foodbank.webapp.repositories.RecipientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectionHistoryService {
    private final CollectionHistoryRepository collectionHistoryRepository;
    private final RecipientRepository recipientRepository;

    public List<CollectionHistoryResponse> getHistoryByDonor(Integer donorId) {
        return collectionHistoryRepository.findByDonorId(donorId).stream()
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
                history.getCollectedDatetime().toString(),
                recipientName,
                history.getPickupLocation()
        );
    }
}