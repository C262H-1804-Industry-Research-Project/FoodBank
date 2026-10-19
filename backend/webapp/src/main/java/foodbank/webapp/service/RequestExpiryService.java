package foodbank.webapp.service;

import foodbank.webapp.entity.Request;
import foodbank.webapp.repositories.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestExpiryService {

    private final RequestRepository requestRepository;

    /**
     * Once a pickup slot has ended, a request the donor never reviewed is rejected
     * and an approved request that was not collected becomes EXPIRED.
     * The donor can still mark an EXPIRED request as COMPLETED.
     * Pickup places are not released here.
     */
    @Scheduled(fixedDelayString = "${REQUEST_EXPIRY_CHECK_MS:60000}")
    @Transactional
    public void closeEndedRequests() {
        List<Request> ended = requestRepository.findSlotEndedByStatusIn(
                List.of("PENDING", "APPROVED"), LocalDateTime.now());

        for (Request request : ended) {
            if ("PENDING".equals(request.getStatus())) {
                request.setStatus("REJECTED");
                request.setRejectionReason(RequestService.NOT_REVIEWED_IN_TIME);
            } else {
                request.setStatus("EXPIRED");
            }
        }
        requestRepository.saveAll(ended);
    }
}
