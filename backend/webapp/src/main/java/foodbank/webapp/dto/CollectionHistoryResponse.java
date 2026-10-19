// dto/CollectionHistoryResponse.java
package foodbank.webapp.dto;

public record CollectionHistoryResponse(
        Integer historyId,
        String category,
        Float quantityCollected,
        String collectedDatetime,
        String recipientName,
        String pickupLocation,
        String foodName,
        String status
) {}
