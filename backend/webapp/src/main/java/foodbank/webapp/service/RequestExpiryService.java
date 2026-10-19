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
    private final HandoverPolicy handoverPolicy;

    /**
     * Once a pickup slot has ended, a request the donor never reviewed is rejected.
     * An approved request that was not collected by the handover deadline
     * (slot end plus the grace period) becomes EXPIRED for good.
     * Pickup places are not released here.
     */
    @Scheduled(fixedDelayString = "${REQUEST_EXPIRY_CHECK_MS:60000}")
    @Transactional
    public void closeEndedRequests() {
        LocalDateTime now = LocalDateTime.now();

        List<Request> unreviewed = requestRepository.findSlotEndedByStatusIn(List.of("PENDING"), now);
        for (Request request : unreviewed) {
            request.setStatus("REJECTED");
            request.setRejectionReason(RequestService.NOT_REVIEWED_IN_TIME);
        }
        requestRepository.saveAll(unreviewed);

        List<Request> uncollected = requestRepository.findSlotEndedByStatusIn(
                List.of("APPROVED"), handoverPolicy.expiryCutoff(now));
        for (Request request : uncollected) {
            request.setStatus("EXPIRED");
        }
        requestRepository.saveAll(uncollected);
    }
}
