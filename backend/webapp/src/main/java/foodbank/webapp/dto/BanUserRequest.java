package foodbank.webapp.dto;

import java.time.LocalDate;

public record BanUserRequest(
        Integer reportId,
        String reason,
        String banType,
        LocalDate endDate
) {
}