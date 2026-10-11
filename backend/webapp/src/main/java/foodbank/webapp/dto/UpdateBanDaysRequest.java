package foodbank.webapp.dto;

public record UpdateBanDaysRequest(
        Integer first,
        Integer second,
        Integer third
) {
}