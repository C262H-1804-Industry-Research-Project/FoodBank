package foodbank.webapp.dto;

import java.time.LocalDateTime;

public record CreateTimeslotRequest(
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer maxRecipients
) {
}
