package foodbank.webapp.service;

import foodbank.webapp.dto.CollectionHistoryResponse;
import foodbank.webapp.dto.RecipientCollectionHistoryResponse;
import foodbank.webapp.entity.CollectionHistory;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.exceptions.ForbiddenException;
import foodbank.webapp.repositories.CollectionHistoryRepository;
<<<<<<< HEAD
=======
import foodbank.webapp.repositories.DonorRepository;
>>>>>>> a1ad0e0 (Add paginated recipient collection history API integration)
import foodbank.webapp.repositories.ListingRepository;
import foodbank.webapp.repositories.RecipientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectionHistoryService {

    private final CollectionHistoryRepository
            collectionHistoryRepository;

    private final RecipientRepository recipientRepository;
    private final ListingRepository listingRepository;

<<<<<<< HEAD
    @Transactional(readOnly = true)
    public List<CollectionHistoryResponse> getHistoryByDonor(Integer donorId) {
        return collectionHistoryRepository.findByDonorIdOrderByCollectedDatetimeDescHistoryIdDesc(donorId).stream()
=======
    // Used to provide the food name for recipient history.
    private final ListingRepository listingRepository;

    // Used to provide the donor organisation name.
    private final DonorRepository donorRepository;

    /**
     * Existing donor collection-history function.
     */
    public List<CollectionHistoryResponse> getHistoryByDonor(
            Integer donorId
    ) {
        return collectionHistoryRepository
                .findByDonorId(donorId)
                .stream()
>>>>>>> a1ad0e0 (Add paginated recipient collection history API integration)
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns one page of collection history belonging to the
     * currently authenticated recipient.
     *
     * The user ID comes from the JWT and is converted to the
     * corresponding recipient ID before querying the database.
     */
    public Page<RecipientCollectionHistoryResponse>
    getHistoryByRecipientUserId(
            Integer userId,
            Pageable pageable
    ) {
        Recipient recipient = recipientRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ForbiddenException(
                                "Recipient profile does not exist"
                        )
                );

        return collectionHistoryRepository
                .findByRecipientIdOrderByCollectedDatetimeDesc(
                        recipient.getRecipientId(),
                        pageable
                )
                .map(this::toRecipientResponse);
    }

    /**
     * Converts one history entity into the response required
     * by the recipient collection-history frontend.
     */
    private RecipientCollectionHistoryResponse
    toRecipientResponse(CollectionHistory history) {

        String foodName = null;

        if (history.getListingId() != null) {
            foodName = listingRepository
                    .findById(history.getListingId())
                    .map(listing -> listing.getName())
                    .orElse(null);
        }

        String donorName = null;

        if (history.getDonorId() != null) {
            donorName = donorRepository
                    .findById(history.getDonorId())
                    .map(donor -> donor.getOrgName())
                    .orElse(null);
        }

        String collectedDatetime =
                history.getCollectedDatetime() == null
                        ? null
                        : history.getCollectedDatetime().toString();

        return new RecipientCollectionHistoryResponse(
                history.getHistoryId(),
                history.getRequestId(),
                history.getListingId(),
                foodName,
                history.getCategory(),
                history.getQuantityCollected(),
                donorName,
                collectedDatetime,
                history.getPickupLocation()
        );
    }

    /**
     * Existing conversion method used by donor history.
     */
    private CollectionHistoryResponse toResponse(
            CollectionHistory history
    ) {
        String recipientName = null;

        if (history.getRecipientId() != null) {
            recipientName = recipientRepository
                    .findById(history.getRecipientId())
                    .map(Recipient::getName)
                    .orElse(null);
        }

        String collectedDatetime =
                history.getCollectedDatetime() == null
                        ? null
                        : history.getCollectedDatetime().toString();

        return new CollectionHistoryResponse(
                history.getHistoryId(),
                history.getCategory(),
                history.getQuantityCollected(),
<<<<<<< HEAD
                history.getCollectedDatetime() == null ? null : history.getCollectedDatetime().toString(),
=======
                collectedDatetime,
>>>>>>> a1ad0e0 (Add paginated recipient collection history API integration)
                recipientName,
                history.getPickupLocation(),
                history.getListingId() == null ? null : listingRepository.findById(history.getListingId())
                        .map(Listing::getName).orElse(null),
                history.getStatus() == null || history.getStatus().isBlank()
                        ? "COMPLETED" : history.getStatus().toUpperCase(java.util.Locale.ROOT)
        );
    }
}
