package foodbank.webapp.service;


import foodbank.webapp.dto.ListingResponse;
import foodbank.webapp.dto.TimeSlot;
import foodbank.webapp.dto.CreateListingRequest;
import foodbank.webapp.dto.CreateTimeslotRequest;
import foodbank.webapp.dto.CreatedListingResponse;
import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.ListingTag;
import foodbank.webapp.entity.Tag;
import foodbank.webapp.entity.Timeslot;
import foodbank.webapp.repositories.DonorRepository;
import foodbank.webapp.repositories.ListingTagRepository;
import foodbank.webapp.repositories.ListingRepository;
import foodbank.webapp.repositories.TagRepository;
import foodbank.webapp.repositories.TimeslotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ListingService {
    private static final Set<String> ALLOWED_CATEGORIES = Set.of("bakery", "produce", "pantry");
    private static final Set<String> ALLOWED_TAGS = Set.of("no pork", "nut free", "vegetarian", "halal");
    private final ListingRepository listingRepository;
    private final ListingTagRepository listingTagRepository;
    private final TimeslotRepository timeslotRepository;
    private final DonorRepository donorRepository;
    private final TagRepository tagRepository;

    public Page<Listing> findAllListing(Pageable pageable){
        return listingRepository.findAll(pageable);
    }

    public Page<ListingResponse> findAllListingResponse(Pageable pageable){
        return findAllListing(pageable).map(listing -> new ListingResponse(
                listing.getName(),
                listing.getDescription(),
                listing.getCategory(),
                listing.getExpiryDate() == null ? null : listing.getExpiryDate().toString(),
                listing.getCollectionDeadline() == null ? null : listing.getCollectionDeadline().toString(),
                listingTagRepository.findByListing(listing).stream()
                        .map(listingTag -> listingTag.getTag().getName())
                        .toArray(String[]::new),
                timeslotRepository.findByListing(listing).stream()
                        .map(timeslot -> new TimeSlot(
                                timeslot.getStartTime().toString(),
                                timeslot.getEndTime().toString(),
                                timeslot.getRemainingSlot()
                        ))
                        .toArray(TimeSlot[]::new)
        ));
    }

    @Transactional
    public CreatedListingResponse createListing(Integer donorId, CreateListingRequest request) {
        requireText(request.name(), "name");
        requireText(request.category(), "category");
        requireText(request.pickupAddress(), "pickupAddress");
        if (!ALLOWED_CATEGORIES.contains(request.category())) {
            throw new IllegalArgumentException("category must be one of: bakery, produce, pantry");
        }
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (request.expiryDate() != null && request.collectionDeadline() != null
                && request.collectionDeadline().isAfter(request.expiryDate())) {
            throw new IllegalArgumentException("collectionDeadline cannot be after expiryDate");
        }

        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Donor not found"));
        Listing listing = listingRepository.save(Listing.builder()
                .donor(donor)
                .name(request.name().trim())
                .description(blankToNull(request.description()))
                .category(request.category().trim())
                .quantity(request.quantity())
                .expiryDate(request.expiryDate())
                .collectionDeadline(request.collectionDeadline())
                .pickupAddress(request.pickupAddress().trim())
                .status("AVAILABLE")
                .maxDailyCapacity(request.maxDailyCapacity())
                .photoUrl(blankToNull(request.photoUrl()))
                .build());

        for (String tagName : request.tags() == null ? Collections.<String>emptyList() : request.tags()) {
            if (tagName == null || tagName.isBlank()) {
                throw new IllegalArgumentException("tags cannot contain blank values");
            }
            if (!ALLOWED_TAGS.contains(tagName)) {
                throw new IllegalArgumentException("tag must be one of: no pork, nut free, vegetarian, halal");
            }
            Tag tag = tagRepository.findByNameIgnoreCase(tagName)
                    .orElseThrow(() -> new IllegalStateException("Approved tag catalogue has not been initialized"));
            listingTagRepository.save(ListingTag.builder().listing(listing).tag(tag).build());
        }
        for (CreateTimeslotRequest timeslotRequest : request.timeslots() == null
                ? Collections.<CreateTimeslotRequest>emptyList() : request.timeslots()) {
            saveTimeslot(listing, timeslotRequest);
        }

        return new CreatedListingResponse(listing.getListingId(), donor.getId(), listing.getStatus());
    }

    private void saveTimeslot(Listing listing, CreateTimeslotRequest request) {
        if (request == null || request.startTime() == null || request.endTime() == null || request.maxRecipients() == null) {
            throw new IllegalArgumentException("Each timeslot requires startTime, endTime and maxRecipients");
        }
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("timeslot endTime must be after startTime");
        }
        if (request.maxRecipients() <= 0) {
            throw new IllegalArgumentException("timeslot maxRecipients must be greater than zero");
        }
        timeslotRepository.save(Timeslot.builder()
                .listing(listing)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .maxRecipients(request.maxRecipients())
                .remainingSlot(request.maxRecipients())
                .build());
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
