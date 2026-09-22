package foodbank.webapp.service;


import foodbank.webapp.dto.ListingResponse;
import foodbank.webapp.dto.MyListingResponse;
import foodbank.webapp.dto.TimeSlot;
import foodbank.webapp.dto.CatalogueItemResponse;
import foodbank.webapp.dto.CreateListingRequest;
import foodbank.webapp.dto.CreateTimeslotRequest;
import foodbank.webapp.dto.CreatedListingResponse;
import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Category;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.ListingTag;
import foodbank.webapp.entity.Tag;
import foodbank.webapp.entity.Timeslot;
import foodbank.webapp.repositories.DonorRepository;
import foodbank.webapp.repositories.CategoryRepository;
import foodbank.webapp.repositories.ListingTagRepository;
import foodbank.webapp.repositories.ListingRepository;
import foodbank.webapp.repositories.TagRepository;
import foodbank.webapp.repositories.TimeslotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListingService {
    private final ListingRepository listingRepository;
    private final ListingTagRepository listingTagRepository;
    private final TimeslotRepository timeslotRepository;
    private final DonorRepository donorRepository;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;

    public Page<Listing> findAllListing(Pageable pageable){
        return listingRepository.findAll(pageable);
    }

    public Page<ListingResponse> findAllListingResponse(Pageable pageable){
        return findAllListing(pageable).map(this::toListingResponse);
    }

    public Page<ListingResponse> searchListingResponses(
            String foodName,
            Integer categoryId,
            List<Integer> tagIds,
            Pageable pageable
    ) {
        Specification<Listing> specification = (root, query, builder) -> builder.conjunction();
        if (foodName != null && !foodName.isBlank()) {
            String namePattern = "%" + foodName.trim().toLowerCase() + "%";
            specification = specification.and((root, query, builder) ->
                    builder.like(builder.lower(root.get("name")), namePattern));
        }
        if (categoryId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("category").get("categoryId"), categoryId));
        }
        if (tagIds != null && !tagIds.isEmpty()) {
            specification = specification.and((root, query, builder) -> {
                var matchingTag = query.subquery(Integer.class);
                var listingTag = matchingTag.from(ListingTag.class);
                matchingTag.select(builder.literal(1));
                matchingTag.where(
                        builder.equal(listingTag.get("listing"), root),
                        listingTag.get("tag").get("tagId").in(tagIds)
                );
                return builder.exists(matchingTag);
            });
        }
        return listingRepository.findAll(specification, pageable).map(this::toListingResponse);
    }

    private ListingResponse toListingResponse(Listing listing) {
        return new ListingResponse(
                listing.getListingId(),
                listing.getName(),
                listing.getDescription(),
                listing.getCategory() == null ? null : listing.getCategory().getName(),
                listing.getExpiryDate() == null ? null : listing.getExpiryDate().toString(),
                listing.getMaxDailyCapacity(),
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
                        .toArray(TimeSlot[]::new),
                listing.getDonor() == null ? null : listing.getDonor().getOrgName(),
                listing.getSuburb(),
                listing.getRegion()
        );
    }

    public List<MyListingResponse> getMyListings(Integer donorId) {
        return listingRepository.findByDonorId(donorId).stream()
                .map(this::toMyListingResponse)
                .toList();
    }

    private MyListingResponse toMyListingResponse(Listing listing) {
        return new MyListingResponse(
                listing.getListingId(),
                listing.getName(),
                listing.getCategory() == null ? null : listing.getCategory().getName(),
                listing.getQuantity(),
                listing.getStatus(),
                listing.getExpiryDate() == null ? null : listing.getExpiryDate().toString(),
                listing.getCollectionDeadline() == null ? null : listing.getCollectionDeadline().toString(),
                listing.getPhotoUrl(),
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
        );
    }

    public List<CatalogueItemResponse> findAvailableCategories() {
        return categoryRepository.findAll().stream()
                .map(category -> new CatalogueItemResponse(category.getCategoryId(), category.getName()))
                .toList();
    }

    public List<CatalogueItemResponse> findAvailableTags() {
        return tagRepository.findAll().stream()
                .map(tag -> new CatalogueItemResponse(tag.getTagId(), tag.getName()))
                .toList();
    }

    @Transactional
    public CreatedListingResponse createListing(Integer donorId, CreateListingRequest request) {
        validateListingRequest(request);

        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Donor not found"));
        Listing listing = Listing.builder()
                .donor(donor)
                .status("AVAILABLE")
                .build();
        applyListingRequest(listing, donor, request);
        listing = listingRepository.save(listing);

        replaceListingDetails(listing, request);

        return new CreatedListingResponse(listing.getListingId(), donor.getId(), listing.getStatus());
    }

    @Transactional
    public CreatedListingResponse updateListing(Integer donorId, Integer listingId, CreateListingRequest request) {
        validateListingRequest(request);

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Listing not found"));
        if (!listing.getDonor().getId().equals(donorId)) {
            throw new java.util.NoSuchElementException("Listing not found for donor");
        }

        applyListingRequest(listing, listing.getDonor(), request);
        replaceListingDetails(listing, request);

        return new CreatedListingResponse(listing.getListingId(), donorId, listing.getStatus());
    }

    @Transactional
    public void deleteListing(Integer donorId, Integer listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Listing not found"));
        if (!listing.getDonor().getId().equals(donorId)) {
            throw new java.util.NoSuchElementException("Listing not found for donor");
        }

        listingTagRepository.deleteAll(listingTagRepository.findByListing(listing));
        timeslotRepository.deleteAll(timeslotRepository.findByListing(listing));
        listingRepository.delete(listing);
    }

    private void applyListingRequest(Listing listing, Donor donor, CreateListingRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Selected category does not exist"));

        String addressLine = request.addressLine();
        String suburb = request.suburb();
        String region = request.region();
        if (addressLine == null || addressLine.isBlank()) {
            addressLine = donor.getAddressLine();
            suburb = donor.getSuburb();
            region = donor.getRegion();
        }
        requireText(addressLine, "addressLine");
        requireText(suburb, "suburb");
        requireText(region, "region");

        listing.setName(request.name().trim());
        listing.setDescription(blankToNull(request.description()));
        listing.setCategory(category);
        listing.setQuantity(request.quantity());
        listing.setExpiryDate(request.expiryDate());
        listing.setCollectionDeadline(request.collectionDeadline());
        listing.setAddressLine(addressLine.trim());
        listing.setSuburb(suburb.trim());
        listing.setRegion(region.trim());
        listing.setMaxDailyCapacity(request.maxDailyCapacity());
        listing.setPhotoUrl(blankToNull(request.photoUrl()));
        listing.setIsAutoApproved(request.isAutoApproved());
        listing.setUsePickupCode(request.usePickupCode());
    }

    private void replaceListingDetails(Listing listing, CreateListingRequest request) {
        listingTagRepository.deleteAll(listingTagRepository.findByListing(listing));
        timeslotRepository.deleteAll(timeslotRepository.findByListing(listing));

        for (Integer tagId : request.tagIds() == null ? Collections.<Integer>emptyList() : request.tagIds()) {
            if (tagId == null) {
                throw new IllegalArgumentException("tagIds cannot contain null values");
            }
            Tag tag = tagRepository.findById(tagId)
                    .orElseThrow(() -> new IllegalArgumentException("Selected tag does not exist"));
            listingTagRepository.save(ListingTag.builder().listing(listing).tag(tag).build());
        }

        for (CreateTimeslotRequest timeslotRequest : request.timeslots() == null
                ? Collections.<CreateTimeslotRequest>emptyList() : request.timeslots()) {
            saveTimeslot(listing, timeslotRequest);
        }
    }

    private static void validateListingRequest(CreateListingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        requireText(request.name(), "name");
        if (request.categoryId() == null) {
            throw new IllegalArgumentException("categoryId is required");
        }
        if (request.isAutoApproved() == null) {
            throw new IllegalArgumentException("isAutoApproved is required");
        }
        if (request.usePickupCode() == null) {
            throw new IllegalArgumentException("usePickupCode is required");
        }
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (request.expiryDate() != null && request.collectionDeadline() != null
                && request.collectionDeadline().isAfter(request.expiryDate())) {
            throw new IllegalArgumentException("collectionDeadline cannot be after expiryDate");
        }
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
