package foodbank.webapp.service;

import foodbank.webapp.dto.RecipientReportableRequestResponse;
import foodbank.webapp.dto.ReportableRequestResponse;
import foodbank.webapp.dto.RequestResponse;
import foodbank.webapp.dto.RequestSubmission;
import foodbank.webapp.entity.CollectionHistory;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.entity.Request;
import foodbank.webapp.entity.Timeslot;
import foodbank.webapp.exceptions.ConflictException;
import foodbank.webapp.exceptions.ForbiddenException;
import foodbank.webapp.repositories.CollectionHistoryRepository;
import foodbank.webapp.repositories.ListingRepository;
import foodbank.webapp.repositories.RecipientRepository;
import foodbank.webapp.repositories.RequestRepository;
import foodbank.webapp.repositories.TimeslotRepository;
import foodbank.webapp.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class RequestService {
    public static final int MAX_NOTE_LENGTH = 500;
    // Statuses a donor or recipient can file a report against
    public static final List<String> REPORTABLE_STATUSES = List.of("APPROVED", "COMPLETED", "EXPIRED");
    public static final String DECLINED_BY_DONOR = "DECLINED_BY_DONOR";
    public static final String NOT_REVIEWED_IN_TIME = "NOT_REVIEWED_IN_TIME";

    private final RequestRepository requestRepository;
    private final TimeslotRepository timeslotRepository;
    private final CollectionHistoryRepository collectionHistoryRepository;
    private final RecipientRepository recipientRepository;
    private final UserRepository userRepository;
    private final HandoverPolicy handoverPolicy;
    private final ListingRepository listingRepository;
    private final ListingStockService listingStockService;

    public List<RequestResponse> getDonorRequests(Integer donorId) {
        return requestRepository.findAllByDonor(donorId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ReportableRequestResponse> getReportableRequests(Integer donorId) {
        return requestRepository.findByDonorAndStatusIn(donorId, REPORTABLE_STATUSES).stream()
                .map(this::toReportableResponse)
                .toList();
    }

    @Transactional
    public RequestResponse approveRequest(Integer donorId, Integer requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Request not found"));

        Timeslot timeslot = timeslotRepository.findById(request.getTimeslotId())
                .orElseThrow(() -> new NoSuchElementException("Timeslot not found"));
        checkDonorRequest(donorId, request, timeslot, List.of("PENDING"));
        checkSlotNotEnded(timeslot);
        Listing listing = timeslot.getListing();

        request.setStatus("APPROVED");

        if (Boolean.TRUE.equals(listing.getUsePickupCode())) {
            request.setPickupCode(generatePickupCode());
        }

        return toResponse(requestRepository.save(request));
    }

    @Transactional
    public RequestResponse rejectRequest(Integer donorId, Integer requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Request not found"));
        Timeslot timeslot = timeslotRepository.findById(request.getTimeslotId())
                .orElseThrow(() -> new NoSuchElementException("Timeslot not found"));
        checkDonorRequest(donorId, request, timeslot, List.of("PENDING"));
        checkSlotNotEnded(timeslot);
        request.setStatus("REJECTED");
        request.setRejectionReason(DECLINED_BY_DONOR);
        timeslot.setRemainingSlot(timeslot.getRemainingSlot() + 1);
        timeslotRepository.save(timeslot);
        return toResponse(requestRepository.save(request));
    }

    // Donor confirms the recipient collected the food, up to the handover deadline; the pickup place stays used
    @Transactional
    public RequestResponse completeRequest(Integer donorId, Integer requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Request not found"));
        Timeslot timeslot = timeslotRepository.findById(request.getTimeslotId())
                .orElseThrow(() -> new NoSuchElementException("Timeslot not found"));
        checkDonorRequest(donorId, request, timeslot, List.of("APPROVED"));
        if (LocalDateTime.now().isAfter(handoverPolicy.deadline(timeslot))) {
            throw new ConflictException("The handover window has ended");
        }

        Listing listing = timeslot.getListing();
        request.setStatus("COMPLETED");
        request = requestRepository.save(request);

        CollectionHistory history = CollectionHistory.builder()
                .requestId(request.getRequestId())
                .listingId(listing.getListingId())
                .donorId(listing.getDonor().getId())
                .recipientId(request.getRecipientId())
                .category(listing.getCategory() == null
                        ? null
                        : listing.getCategory().getName())
                .quantityCollected(request.getQuantityRequested())
                .collectedDatetime(LocalDateTime.now())
                .pickupLocation(formatAddress(listing))
                .build();
        collectionHistoryRepository.save(history);

        return toResponse(request);
    }

    private void checkDonorRequest(Integer donorId, Request request, Timeslot timeslot, List<String> expectedStatuses) {
        if (!donorId.equals(timeslot.getListing().getDonor().getId())) {
            throw new ForbiddenException("The pickup request belongs to a different donor");
        }
        if (!expectedStatuses.contains(request.getStatus())) {
            throw new ConflictException("The pickup request has already been processed or cancelled");
        }
    }

    // Covers the gap before RequestExpiryService picks up a request whose slot has ended
    private void checkSlotNotEnded(Timeslot timeslot) {
        if (!timeslot.getEndTime().isAfter(LocalDateTime.now())) {
            throw new ConflictException("The pickup timeslot has already ended");
        }
    }

    @Transactional
    public RequestResponse createRequest(RequestSubmission req, Integer userId) {
        if (req.timeslotId() == null) {
            throw new IllegalArgumentException("timeslotId is required");
        }
        if (req.quantityRequested() == null || !Float.isFinite(req.quantityRequested()) || req.quantityRequested() <= 0) {
            throw new IllegalArgumentException("quantityRequested must be greater than zero");
        }
        String note = req.note() == null || req.note().isBlank() ? null : req.note().strip();
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("note must be at most " + MAX_NOTE_LENGTH + " characters");
        }

        Timeslot timeslot = timeslotRepository.findById(req.timeslotId())
                .orElseThrow(() -> new NoSuchElementException("Timeslot not found"));

        if (!timeslot.getStartTime().isAfter(LocalDateTime.now())) {
            throw new ConflictException("The pickup timeslot has already started");
        }

        Recipient recipient = findRecipientForUser(userId);
        Integer recipientId = recipient.getRecipientId();

        Listing listing = listingRepository.findByIdForUpdate(timeslot.getListing().getListingId())
                .orElseThrow(() -> new NoSuchElementException("Listing not found"));

        if (!"LIVE".equals(listing.getStatus())) {
            throw new ConflictException("This listing is not currently available for requests");
        }

        if (timeslot.getRemainingSlot() == null || timeslot.getRemainingSlot() <= 0) {
            throw new ConflictException("This timeslot is fully booked");
        }

        boolean alreadyRequested = requestRepository.existsByTimeslotIdAndRecipientIdAndStatusNot(
                req.timeslotId(), recipientId, "CANCELLED");
        if (alreadyRequested) {
            throw new ConflictException("You have already submitted a request for this timeslot");
        }

        LocalDate pickupDay = timeslot.getStartTime().toLocalDate();
        LocalDateTime dayStart = pickupDay.atStartOfDay();
        LocalDateTime dayEnd = pickupDay.plusDays(1).atStartOfDay();

        Integer listingLimit = listing.getMaxDailyCapacity();
        if (listingLimit != null) {
            Double usedForListing = requestRepository.sumActiveQuantityForListing(
                    recipientId, listing.getListingId(), dayStart, dayEnd);
            double totalForListing = (usedForListing == null ? 0 : usedForListing) + req.quantityRequested();
            if (totalForListing > listingLimit) {
                throw new IllegalArgumentException(
                        "Exceeds the daily limit of " + listingLimit + " for this listing");
            }
        }

        Integer personalLimit = recipient.getClaimLimit();
        if (personalLimit != null) {
            Double usedForDay = requestRepository.sumActiveQuantityForDay(
                    recipientId, dayStart, dayEnd);
            double totalForDay = (usedForDay == null ? 0 : usedForDay) + req.quantityRequested();
            if (totalForDay > personalLimit) {
                throw new IllegalArgumentException(
                        "Exceeds your personal daily limit of " + personalLimit);
            }
        }

        // A request holds its quantity as soon as it is made, even before approval
        listingStockService.requireAvailable(listing, req.quantityRequested());

        boolean autoApproved = Boolean.TRUE.equals(listing.getIsAutoApproved());
        boolean needsPickupCode = Boolean.TRUE.equals(listing.getUsePickupCode());

        Request request = Request.builder()
                .timeslotId(req.timeslotId())
                .recipientId(recipientId)
                .quantityRequested(req.quantityRequested())
                .status(autoApproved ? "APPROVED" : "PENDING")
                .pickupCode(autoApproved && needsPickupCode ? generatePickupCode() : null)
                .note(note)
                .build();

        request = requestRepository.save(request);

        timeslot.setRemainingSlot(timeslot.getRemainingSlot() - 1);
        timeslotRepository.save(timeslot);

        return toResponse(request);
    }

    private String generatePickupCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    public List<RequestResponse> getMyRequests(Integer userId) {
        Integer recipientId = findRecipientForUser(userId).getRecipientId();
        return requestRepository.findByRecipientId(recipientId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<RecipientReportableRequestResponse> getRecipientReportableRequests(Integer userId) {
        Integer recipientId = findRecipientForUser(userId).getRecipientId();
        return requestRepository.findByRecipientIdAndStatusIn(recipientId, REPORTABLE_STATUSES).stream()
                .map(this::toRecipientReportableResponse)
                .toList();
    }

    @Transactional
    public void cancelRequest(Integer requestId, Integer userId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Request not found"));
        Recipient recipient = findRecipientForUser(userId);
        if (!request.getRecipientId().equals(recipient.getRecipientId())) {
            throw new ForbiddenException("You can only cancel your own requests");
        }
        if (!"PENDING".equals(request.getStatus()) && !"APPROVED".equals(request.getStatus())) {
            throw new ConflictException("This request can no longer be cancelled");
        }

        Timeslot timeslot = timeslotRepository.findById(request.getTimeslotId())
                .orElseThrow(() -> new NoSuchElementException("Timeslot not found"));
        if (!timeslot.getStartTime().isAfter(LocalDateTime.now())) {
            throw new ConflictException("Requests can only be cancelled before pickup time");
        }

        request.setStatus("CANCELLED");
        request.setPickupCode(null);
        requestRepository.save(request);
        timeslot.setRemainingSlot(timeslot.getRemainingSlot() + 1);
        timeslotRepository.save(timeslot);
    }

    private Recipient findRecipientForUser(Integer userId) {
        return recipientRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Recipient profile does not exist"));
    }

    private String formatAddress(Listing listing) {
        String street = listing.getAddressLine();
        if (street == null || street.isBlank()) street = listing.getDonor().getAddressLine();
        return java.util.stream.Stream.of(street, listing.getSuburb(), listing.getRegion())
                .filter(part -> part != null && !part.isBlank())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private RequestResponse toResponse(Request request) {
        Timeslot timeslot = timeslotRepository.findById(request.getTimeslotId())
                .orElseThrow(() -> new NoSuchElementException("Timeslot not found"));
        Recipient recipient = recipientRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new NoSuchElementException("Recipient not found"));
        Listing listing = timeslot.getListing();

        return new RequestResponse(
                request.getRequestId(),
                recipient.getName(),
                listing.getName(),
                timeslot.getStartTime().toString(),
                timeslot.getEndTime().toString(),
                request.getQuantityRequested(),
                request.getStatus(),
                request.getPickupCode(),
                request.getNote(),
                listing.getDonor().getOrgName(),
                "APPROVED".equals(request.getStatus()) ? formatAddress(listing) : null,
                recipient.getOrgName(),
                userRepository.findById(recipient.getUserId()).map(user -> user.getPhone()).orElse(null),
                listing.getQuantity(),
                listingStockService.available(listing),
                "APPROVED".equals(request.getStatus()) || "EXPIRED".equals(request.getStatus())
                        ? handoverPolicy.deadline(timeslot).toString() : null,
                "REJECTED".equals(request.getStatus())
                        ? (request.getRejectionReason() == null ? DECLINED_BY_DONOR : request.getRejectionReason())
                        : null
        );
    }

    private ReportableRequestResponse toReportableResponse(Request request) {
        Timeslot timeslot = timeslotRepository.findById(request.getTimeslotId())
                .orElseThrow(() -> new NoSuchElementException("Timeslot not found"));
        Recipient recipient = recipientRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new NoSuchElementException("Recipient not found"));
        Listing listing = timeslot.getListing();

        return new ReportableRequestResponse(
                request.getRequestId(),
                listing.getListingId(),
                listing.getName(),
                timeslot.getTimeslotId(),
                timeslot.getStartTime().toString(),
                timeslot.getEndTime().toString(),
                recipient.getUserId(),
                recipient.getName(),
                request.getQuantityRequested()
        );
    }

    private RecipientReportableRequestResponse toRecipientReportableResponse(Request request) {
        Timeslot timeslot = timeslotRepository.findById(request.getTimeslotId())
                .orElseThrow(() -> new NoSuchElementException("Timeslot not found"));
        Listing listing = timeslot.getListing();

        return new RecipientReportableRequestResponse(
                request.getRequestId(),
                listing.getListingId(),
                listing.getName(),
                listing.getDonor() == null ? null : listing.getDonor().getOrgName(),
                timeslot.getStartTime().toString(),
                timeslot.getEndTime().toString(),
                request.getQuantityRequested()
        );
    }
}
