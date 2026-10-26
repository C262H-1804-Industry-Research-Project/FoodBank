package foodbank.webapp.service;

import foodbank.webapp.entity.Request;
import foodbank.webapp.repositories.RequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Expired requests release their listing stock once their pickup day is over:
 * every EXPIRED request whose slot ended at or before the most recent midnight
 * stops holding stock, so later slots of that listing can use the food.
 * The request stays EXPIRED. Same rule for every donor.
 *
 * The rule depends only on the slot's day, never on when it runs, so running it
 * again (or late, after downtime) is always safe and never touches today's slots.
 */
@Service
public class StockReleaseService {

    private final RequestRepository requestRepository;
    // Slot times are stored as Adelaide wall-clock times, so midnight is worked out in the same zone
    private final ZoneId zone;

    public StockReleaseService(RequestRepository requestRepository,
                               @Value("${stock.release-zone:Australia/Adelaide}") String zone) {
        this.requestRepository = requestRepository;
        this.zone = ZoneId.of(zone);
    }

    // 00:20 leaves time for slots ending just before midnight to pass their 15-minute handover deadline and expire
    @Scheduled(cron = "${stock.release-cron:0 20 0 * * *}", zone = "${stock.release-zone:Australia/Adelaide}")
    @Transactional
    public void releaseExpiredStock() {
        LocalDateTime midnight = LocalDate.now(zone).atStartOfDay();
        List<Request> expired = requestRepository.findUnreleasedExpiredWithSlotEndedBy(midnight);
        LocalDateTime now = LocalDateTime.now(zone);
        for (Request request : expired) {
            request.setStockReleasedAt(now);
        }
        requestRepository.saveAll(expired);
    }

    // Catch up on a nightly run missed while the server was down
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void releaseOnStartup() {
        releaseExpiredStock();
    }
}
