package foodbank.webapp.dto;

import java.time.LocalDate;

public record BanUserResponse(
        String message,
        Integer banId,
        Integer userId,
        Integer adminId,
        Integer reportId,
        String reason,
        String banType,
        Integer banCount,
        LocalDate startDate,
        LocalDate endDate,
        String userStatus
) {
}