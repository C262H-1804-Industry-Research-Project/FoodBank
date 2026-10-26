package foodbank.webapp.dto;

public record LinkAccountRequest(String targetRole, String email, String password) {}
