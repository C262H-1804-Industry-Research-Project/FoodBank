package foodbank.webapp.dto;

public record TimeSlot(
        Integer timeslotId,
        String startTime,
        String endTime,
        int remainingSlot,
        int maxRecipients
){}
