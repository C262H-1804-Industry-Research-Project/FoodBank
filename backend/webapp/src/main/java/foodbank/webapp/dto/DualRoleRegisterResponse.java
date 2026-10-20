package foodbank.webapp.dto;

public record DualRoleRegisterResponse(
        Integer userId,
        Integer donorId,
        Integer recipientId,
        String email,
        String donorApprovalStatus,
        String recipientApprovalStatus
) {
}