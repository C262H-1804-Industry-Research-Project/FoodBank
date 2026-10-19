package foodbank.webapp.service;

import foodbank.webapp.entity.Timeslot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * How long after a pickup slot ends the donor can still confirm a handover.
 * One app-wide value for now; a per-donor setting can replace it here later.
 */
@Component
public class HandoverPolicy {

    private final Duration grace;

    public HandoverPolicy(@Value("${pickup.handover-grace-minutes:15}") long graceMinutes) {
        this.grace = Duration.ofMinutes(graceMinutes);
    }

    public LocalDateTime deadline(Timeslot timeslot) {
        return timeslot.getEndTime().plus(grace);
    }

    // Approved requests whose slot ended at or before this time are past their handover deadline
    public LocalDateTime expiryCutoff(LocalDateTime now) {
        return now.minus(grace);
    }
}
