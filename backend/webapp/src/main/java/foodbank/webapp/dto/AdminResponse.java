package foodbank.webapp.dto;

public record AdminResponse(
        Integer adminId,
        String name,
        String email
) {
}
