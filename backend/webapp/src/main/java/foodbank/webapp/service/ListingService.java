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
import foodbank.webapp.repositories.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Objects;
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
    private final GeocodingService geocodingService;
    private final RequestRepository requestRepository;
    private final ListingStockService listingStockService;

    public Page<Listing> findAllListing(Pageable pageable){
        return listingRepository.findAll(pageable);
    }

    public Page<foodbank.webapp.dto.AdminListingResponse> findAdminListings(Pageable pageable) {
        return findAllListing(pageable).map(listing -> new foodbank.webapp.dto.AdminListingResponse(
                toListingResponse(listing, null), listing.getStatus(), listing.getQuantity(),
                requestRepository.countCompletedForListing(listing.getListingId()),
                requestRepository.countApprovedForListing(listing.getListingId())));
    }

    public Page<ListingResponse> findAllListingResponse(Pageable pageable){
        return findAllListing(pageable).map(listing -> toListingResponse(listing, null));
    }

    public Page<ListingResponse> searchListingResponses(
            String foodName,
            Integer categoryId,
            List<Integer> tagIds,
            List<String> suburbs,
            GeocodingService.Coordinates origin,
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
        if (suburbs != null && !suburbs.isEmpty()) {
            specification = specification.and(inAnySuburb(suburbs));
        }
        if (origin != null) {
            specification = specification.and(nearestFirst(origin));
        }
        return listingRepository.findAll(specification, pageable).map(listing -> toListingResponse(listing, origin));
    }

    /**
     * Matches listings in any of the given suburbs. Each entry is "Suburb|REGION", or just "Suburb"
     * to match that suburb name in any region. Comparison ignores case and surrounding spaces.
     */
    private Specification<Listing> inAnySuburb(List<String> suburbs) {
        return (root, query, builder) -> {
            var listingSuburb = builder.lower(builder.trim(root.<String>get("suburb")));
            var listingRegion = builder.lower(builder.trim(root.<String>get("region")));
            var matches = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            for (String entry : suburbs) {
                if (entry == null || entry.isBlank()) continue;
                String[] parts = entry.split("\\|", 2);
                String suburb = parts[0].trim().toLowerCase();
                String region = parts.length > 1 ? parts[1].trim().toLowerCase() : "";
                if (suburb.isEmpty()) continue;
                matches.add(region.isEmpty()
                        ? builder.equal(listingSuburb, suburb)
                        : builder.and(builder.equal(listingSuburb, suburb), builder.equal(listingRegion, region)));
            }
            return matches.isEmpty()
                    ? builder.conjunction()
                    : builder.or(matches.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    /**
     * Orders by an equirectangular distance approximation, which ranks nearby points the same as the
     * exact great-circle distance while staying plain arithmetic for the database. Listings without
     * coordinates come last. The count query is left unordered.
     */
    private Specification<Listing> nearestFirst(GeocodingService.Coordinates origin) {
        double longitudeScale = Math.cos(Math.toRadians(origin.latitude()));
        return (root, query, builder) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                jakarta.persistence.criteria.Expression<Double> latitude = root.get("latitude");
                jakarta.persistence.criteria.Expression<Double> longitude = root.get("longitude");
                var latitudeDelta = builder.diff(latitude, origin.latitude());
                var longitudeDelta = builder.prod(builder.diff(longitude, origin.longitude()), longitudeScale);
                var distanceSquared = builder.sum(
                        builder.prod(latitudeDelta, latitudeDelta),
                        builder.prod(longitudeDelta, longitudeDelta));
                var missingCoordinates = builder.<Integer>selectCase()
                        .when(builder.or(builder.isNull(latitude), builder.isNull(longitude)), 1)
                        .otherwise(0);
                query.orderBy(
                        builder.asc(missingCoordinates),
                        builder.asc(distanceSquared),
                        builder.asc(root.get("listingId")));
            }
            return builder.conjunction();
        };
    }

    /** Looks up and stores the pickup point coordinates; leaves them empty if the lookup fails. */
    public void updateCoordinates(Listing listing) {
        var coordinates = geocodingService.geocode(listing.getAddressLine(), listing.getSuburb(), listing.getRegion());
        listing.setLatitude(coordinates.map(GeocodingService.Coordinates::latitude).orElse(null));
        listing.setLongitude(coordinates.map(GeocodingService.Coordinates::longitude).orElse(null));
    }

    private ListingResponse toListingResponse(Listing listing, GeocodingService.Coordinates origin) {
        Double distanceKm = origin == null || listing.getLatitude() == null || listing.getLongitude() == null
                ? null
                : GeocodingService.distanceKm(origin.latitude(), origin.longitude(),
                        listing.getLatitude(), listing.getLongitude());
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
                listing.getRegion(),
                distanceKm,
                listingStockService.available(listing)
        );
    }

    public Page<MyListingResponse> getMyListings(Integer donorId, Pageable pageable) {
        return listingRepository.findByDonorIdOrderByListingIdDesc(donorId, pageable)
                .map(this::toMyListingResponse);
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
                listing.getUsePickupCode(), listing.getHideContact(), listing.getSlotCapacity(),
                requestRepository.countCompletedForListing(listing.getListingId()),
                requestRepository.countApprovedForListing(listing.getListingId()),
                listingStockService.available(listing)
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
        updateCoordinates(listing);
        listing = listingRepository.save(listing);

        replaceListingDetails(listing, request);

        return new CreatedListingResponse(listing.getListingId(), donor.getId(), listing.getStatus());
    }

    @Transactional
    public MyListingResponse updateListing(Integer donorId, Integer listingId, foodbank.webapp.dto.UpdateListingRequest request) {
        Listing listing = listingRepository.findByIdForUpdate(listingId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Listing not found"));
        if (!listing.getDonor().getId().equals(donorId)) {
            throw new java.util.NoSuchElementException("Listing not found for donor");
        }

        // An admin-banned listing cannot be edited or reactivated by its donor.
        if ("BANNED".equalsIgnoreCase(listing.getStatus())) {
            throw new foodbank.webapp.exceptions.ConflictException(
                    "Banned listing cannot be updated"
            );
        }

        if (request == null) throw new IllegalArgumentException("request body is required");
        requireText(request.name(), "name");
        // A blank address means "use the donor's own address", as when creating a listing
        String addressLine = request.addressLine();
        String suburb = request.suburb();
        String region = request.region();
        if (addressLine == null || addressLine.isBlank()) {
            addressLine = listing.getDonor().getAddressLine();
            suburb = listing.getDonor().getSuburb();
            region = listing.getDonor().getRegion();
        }
        requireText(addressLine, "addressLine");
        requireText(suburb, "suburb");
        requireText(region, "region");
        if (request.categoryId() == null) throw new IllegalArgumentException("categoryId is required");
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Selected category does not exist"));
        validateQuantity(request.quantity());
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
        float held = listingStockService.held(listing);
        if (request.quantity() < held) {
            throw new foodbank.webapp.exceptions.ConflictException(
                    "Quantity cannot be below reserved stock (" + ListingStockService.format(held) + ")");
        }
        var slots = timeslotRepository.findByListing(listing);
        // Slots the edit removes; refuse before changing anything if any of them is booked
        Set<String> requestedKeys = timeslotKeys(request.timeslots());
        List<Timeslot> removedSlots = slots.stream()
                .filter(slot -> !requestedKeys.contains(timeslotKey(slot.getStartTime(), slot.getEndTime())))
                .toList();
        if (removedSlots.stream().anyMatch(slot -> slot.getRemainingSlot() < slot.getMaxRecipients())) {
            throw new foodbank.webapp.exceptions.ConflictException("Pickup timeslots with bookings cannot be removed");
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
        boolean addressChanged = !Objects.equals(listing.getAddressLine(), addressLine.trim())
                || !Objects.equals(listing.getSuburb(), suburb.trim())
                || !Objects.equals(listing.getRegion(), region.trim());
        listing.setAddressLine(addressLine.trim());
        listing.setSuburb(suburb.trim());
        listing.setRegion(region.trim());
        if (addressChanged || listing.getLatitude() == null) {
            updateCoordinates(listing);
        }
        listing.setMaxDailyCapacity(request.maxDailyCapacity());
        listing.setPhotoUrl(blankToNull(request.photoUrl()));
        listing.setIsAutoApproved(request.isAutoApproved());
        listing.setUsePickupCode(request.usePickupCode());
        listing.setHideContact(request.hideContact());
        listing.setSlotCapacity(request.slotCapacity());
        listing.setStatus(request.status());
        replaceListingTags(listing, request.tagIds());
        updateTimeslots(listing, slots, removedSlots, request.timeslots());
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

    // Applies an edit's timeslots by start and end time: existing slots that are still listed
    // are kept as they are (with their bookings), removed ones are deleted and new ones created.
    private void updateTimeslots(Listing listing, List<Timeslot> slots, List<Timeslot> removedSlots,
                                 List<CreateTimeslotRequest> requestedSlots) {
        timeslotRepository.deleteAll(removedSlots);
        Set<String> existingKeys = new HashSet<>();
        for (Timeslot slot : slots) existingKeys.add(timeslotKey(slot.getStartTime(), slot.getEndTime()));
        for (CreateTimeslotRequest slot : requestedSlots == null
                ? Collections.<CreateTimeslotRequest>emptyList() : requestedSlots) {
            // existingKeys also stops the same new slot being created twice
            if (slot == null || existingKeys.add(timeslotKey(slot.startTime(), slot.endTime()))) {
                saveTimeslot(listing, slot);
            }
        }
    }

    private static Set<String> timeslotKeys(List<CreateTimeslotRequest> requestedSlots) {
        Set<String> keys = new HashSet<>();
        for (CreateTimeslotRequest slot : requestedSlots == null
                ? Collections.<CreateTimeslotRequest>emptyList() : requestedSlots) {
            if (slot != null) keys.add(timeslotKey(slot.startTime(), slot.endTime()));
        }
        return keys;
    }

    private static String timeslotKey(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        return startTime + "|" + endTime;
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
        validateQuantity(request.quantity());
        if (request.expiryDate() != null && request.collectionDeadline() != null
                && request.collectionDeadline().isAfter(request.expiryDate())) {
            throw new IllegalArgumentException("collectionDeadline cannot be after expiryDate");
        }
    }

    private static void validateQuantity(Float quantity) {
        if (quantity == null || !Float.isFinite(quantity) || quantity <= 0 || quantity != Math.floor(quantity)) {
            throw new IllegalArgumentException("quantity must be a whole number greater than zero");
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
