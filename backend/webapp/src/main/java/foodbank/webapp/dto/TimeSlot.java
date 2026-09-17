package foodbank.webapp.dto;

public record TimeSlot(
        String startTime,
        String endTime,
        int remainingSlot
){}
