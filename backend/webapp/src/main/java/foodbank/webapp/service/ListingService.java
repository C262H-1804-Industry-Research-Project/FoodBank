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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        specification = specification.and((root, query, builder) ->
                builder.equal(root.get("status"), "LIVE"));
        
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
                listing.getPhotoUrl(),
                listing.getCategory() == null ? null : listing.getCategory().getName(),
                listing.getExpiryDate() == null ? null : listing.getExpiryDate().toString(),
                listing.getMaxDailyCapacity(),
                listing.getCollectionDeadline() == null ? null : listing.getCollectionDeadline().toString(),
                listingTagRepository.findByListing(listing).stream()
                        .map(listingTag -> listingTag.getTag().getName())
                        .toArray(String[]::new),
                timeslotRepository.findByListing(listing).stream()
                        .map(timeslot -> new TimeSlot(
                                timeslot.getTimeslotId(),
                                timeslot.getStartTime().toString(),
                                timeslot.getEndTime().toString(),
                                timeslot.getRemainingSlot(),
                                timeslot.getMaxRecipients()
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
                listing.getMaxDailyCapacity(),
                listing.getPhotoUrl(),
                listingTagRepository.findByListing(listing).stream()
                        .map(listingTag -> listingTag.getTag().getName())
                        .toArray(String[]::new),
                timeslotRepository.findByListing(listing).stream()
                        .map(timeslot -> new TimeSlot(
                                timeslot.getTimeslotId(),
                                timeslot.getStartTime().toString(),
                                timeslot.getEndTime().toString(),
                                timeslot.getRemainingSlot(),
                                timeslot.getMaxRecipients()
                        ))
                        .toArray(TimeSlot[]::new),
                listing.getSuburb(),
                listing.getRegion(),
                listing.getCategory() == null ? null : listing.getCategory().getCategoryId(),
                listing.getDescription(), listing.getAddressLine(), listing.getIsAutoApproved(),
                listing.getUsePickupCode(), listing.getHideContact(), listing.getSlotCapacity()
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
                .status(request.status())
                .build();
        applyListingRequest(listing, donor, request);
        listing = listingRepository.save(listing);

        replaceListingDetails(listing, request);

        return new CreatedListingResponse(listing.getListingId(), donor.getId(), listing.getStatus());
    }

    @Transactional
    public MyListingResponse updateListing(Integer donorId, Integer listingId, foodbank.webapp.dto.UpdateListingRequest request) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Listing not found"));
        if (!listing.getDonor().getId().equals(donorId)) {
            throw new java.util.NoSuchElementException("Listing not found for donor");
        }
        if (request == null) throw new IllegalArgumentException("request body is required");
        requireText(request.name(), "name");
        requireText(request.addressLine(), "addressLine");
        requireText(request.suburb(), "suburb");
        requireText(request.region(), "region");
        if (request.categoryId() == null) throw new IllegalArgumentException("categoryId is required");
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Selected category does not exist"));
        if (request.quantity() == null || !Float.isFinite(request.quantity()) || request.quantity() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (request.isAutoApproved() == null || request.usePickupCode() == null || request.hideContact() == null) {
            throw new IllegalArgumentException("Approval, pickup code and contact preferences are required");
        }
        if (request.slotCapacity() == null || request.slotCapacity() < 1 || request.slotCapacity() > 20) {
            throw new IllegalArgumentException("Recipients per slot must be between 1 and 20");
        }
        if (request.collectionDeadline() != null && request.expiryDate() != null
                && request.collectionDeadline().isAfter(request.expiryDate())) {
            throw new IllegalArgumentException("collectionDeadline cannot be after expiryDate");
        }
        if (request.maxDailyCapacity() != null && request.maxDailyCapacity() < 1) {
            throw new IllegalArgumentException("maxDailyCapacity must be greater than zero");
        }
        if (!"LIVE".equals(request.status()) && !"DRAFT".equals(request.status())) {
            throw new IllegalArgumentException("status must be LIVE or DRAFT");
        }
        var slots = timeslotRepository.findByListing(listing);
        if (slots.stream().anyMatch(slot -> slot.getRemainingSlot() < slot.getMaxRecipients())
                && !hasSameTimeslots(slots, request.timeslots())) {
            throw new foodbank.webapp.exceptions.ConflictException("Pickup timeslots with bookings cannot be changed");
        }
        Integer previousCapacity = listing.getSlotCapacity();
        if (previousCapacity == null && !slots.isEmpty()) {
            previousCapacity = slots.stream().min(java.util.Comparator.comparing(Timeslot::getStartTime)).orElseThrow().getMaxRecipients();
        }
        // Only an explicit capacity change alters slots; never recreate booked slots.
        if (!request.slotCapacity().equals(previousCapacity)) {
            for (Timeslot slot : slots) {
                int booked = slot.getMaxRecipients() - slot.getRemainingSlot();
                if (request.slotCapacity() < booked) {
                    throw new foodbank.webapp.exceptions.ConflictException("Capacity cannot be below existing bookings");
                }
                slot.setMaxRecipients(request.slotCapacity());
                slot.setRemainingSlot(request.slotCapacity() - booked);
            }
        }
        listing.setName(request.name().trim());
        listing.setDescription(blankToNull(request.description()));
        listing.setCategory(category);
        listing.setQuantity(request.quantity());
        listing.setExpiryDate(request.expiryDate());
        listing.setCollectionDeadline(request.collectionDeadline());
        listing.setAddressLine(request.addressLine().trim());
        listing.setSuburb(request.suburb().trim());
        listing.setRegion(request.region().trim());
        listing.setMaxDailyCapacity(request.maxDailyCapacity());
        listing.setPhotoUrl(blankToNull(request.photoUrl()));
        listing.setIsAutoApproved(request.isAutoApproved());
        listing.setUsePickupCode(request.usePickupCode());
        listing.setHideContact(request.hideContact());
        listing.setSlotCapacity(request.slotCapacity());
        listing.setStatus(request.status());
        replaceListingTags(listing, request.tagIds());
        if (!hasSameTimeslots(slots, request.timeslots())) {
            replaceTimeslots(listing, request.timeslots());
        }
        listingRepository.flush();
        return toMyListingResponse(listing);
    }

    public void requireDonorOwner(Integer donorId, String userId) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Donor not found"));
        if (!donor.getUserId().toString().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Not your listing");
        }
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
        replaceListingTags(listing, request.tagIds());
        replaceTimeslots(listing, request.timeslots());
    }

    private void replaceListingTags(Listing listing, List<Integer> tagIds) {
        listingTagRepository.deleteAll(listingTagRepository.findByListing(listing));
        for (Integer tagId : tagIds == null ? Collections.<Integer>emptyList() : tagIds) {
            if (tagId == null) {
                throw new IllegalArgumentException("tagIds cannot contain null values");
            }
            Tag tag = tagRepository.findById(tagId)
                    .orElseThrow(() -> new IllegalArgumentException("Selected tag does not exist"));
            listingTagRepository.save(ListingTag.builder().listing(listing).tag(tag).build());
        }

    }

    private void replaceTimeslots(Listing listing, List<CreateTimeslotRequest> timeslots) {
        timeslotRepository.deleteAll(timeslotRepository.findByListing(listing));
        for (CreateTimeslotRequest timeslotRequest : timeslots == null
                ? Collections.<CreateTimeslotRequest>emptyList() : timeslots) {
            saveTimeslot(listing, timeslotRequest);
        }
    }

    private static boolean hasSameTimeslots(List<Timeslot> slots, List<CreateTimeslotRequest> requestedSlots) {
        if (slots.size() != (requestedSlots == null ? 0 : requestedSlots.size())) return false;
        Set<String> existing = new HashSet<>();
        for (Timeslot slot : slots) existing.add(slot.getStartTime() + "|" + slot.getEndTime());
        Set<String> requested = new HashSet<>();
        for (CreateTimeslotRequest slot : requestedSlots == null
                ? Collections.<CreateTimeslotRequest>emptyList() : requestedSlots) {
            if (slot == null || slot.startTime() == null || slot.endTime() == null) return false;
            requested.add(slot.startTime() + "|" + slot.endTime());
        }
        return existing.equals(requested) && existing.size() == slots.size();
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
        if (!request.startTime().toLocalDate().equals(request.endTime().toLocalDate())) {
            throw new IllegalArgumentException("timeslot startTime and endTime must be on the same date");
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
