package foodbank.webapp.service;

import foodbank.webapp.dto.RequestResponse;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.entity.Request;
import foodbank.webapp.entity.Timeslot;
import foodbank.webapp.repositories.RecipientRepository;
import foodbank.webapp.repositories.RequestRepository;
import foodbank.webapp.repositories.TimeslotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RequestService {
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
        request.setStatus("APPROVED");
        return toResponse(requestRepository.save(request));
    }

    @Transactional
    public RequestResponse rejectRequest(Integer requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Request not found"));
        request.setStatus("REJECTED");
        return toResponse(requestRepository.save(request));
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
                request.getPickupCode()
        );
    }
}
