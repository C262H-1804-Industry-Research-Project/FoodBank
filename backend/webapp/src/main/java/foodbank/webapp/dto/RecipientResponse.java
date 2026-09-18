package foodbank.webapp.dto;

public record RecipientResponse(
        Integer recipientId,
        Integer userId,
        String email,
        String name,
        String approvalStatus
) {
}