package foodbank.webapp.service;

import foodbank.webapp.dto.RequestResponse;
import foodbank.webapp.dto.RequestSubmission;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.entity.Request;
import foodbank.webapp.entity.Timeslot;
import foodbank.webapp.exceptions.ConflictException;
import foodbank.webapp.exceptions.ForbiddenException;
import foodbank.webapp.repositories.RecipientRepository;
import foodbank.webapp.repositories.RequestRepository;
import foodbank.webapp.repositories.TimeslotRepository;
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

    private final RequestRepository requestRepository;
    private final TimeslotRepository timeslotRepository;
    private final RecipientRepository recipientRepository;

    public List<RequestResponse> getPendingRequests(Integer donorId) {
        return requestRepository.findPendingByDonor(donorId, "PENDING").stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RequestResponse approveRequest(Integer requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Request not found"));

        Timeslot timeslot = timeslotRepository.findById(request.getTimeslotId())
                .orElseThrow(() -> new NoSuchElementException("Timeslot not found"));
        Listing listing = timeslot.getListing();

        request.setStatus("APPROVED");

        if (Boolean.TRUE.equals(listing.getUsePickupCode())) {
            request.setPickupCode(generatePickupCode());
        }

        return toResponse(requestRepository.save(request));
    }

    @Transactional
    public RequestResponse rejectRequest(Integer requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Request not found"));
        request.setStatus("REJECTED");
        return toResponse(requestRepository.save(request));
    }

    @Transactional
    public RequestResponse createRequest(RequestSubmission req, Integer userId) {
        if (req.timeslotId() == null) {
            throw new IllegalArgumentException("timeslotId is required");
        }
        if (req.quantityRequested() == null || req.quantityRequested() <= 0) {
            throw new IllegalArgumentException("quantityRequested must be greater than zero");
        }
        String note = req.note() == null || req.note().isBlank() ? null : req.note().strip();
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("note must be at most " + MAX_NOTE_LENGTH + " characters");
        }

        Timeslot timeslot = timeslotRepository.findById(req.timeslotId())
                .orElseThrow(() -> new NoSuchElementException("Timeslot not found"));

        Recipient recipient = findRecipientForUser(userId);
        Integer recipientId = recipient.getRecipientId();

        Listing listing = timeslot.getListing();

        if (!"LIVE".equals(listing.getStatus())) {
            throw new ConflictException("This listing is not currently available for requests");
        }

        if (timeslot.getRemainingSlot() == null || timeslot.getRemainingSlot() <= 0) {
            throw new ConflictException("This timeslot is fully booked");
        }

        boolean alreadyRequested = requestRepository.existsByTimeslotIdAndRecipientId(
                req.timeslotId(), recipientId);
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

    private Recipient findRecipientForUser(Integer userId) {
        return recipientRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Recipient profile does not exist"));
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
                request.getNote()
        );
    }
}