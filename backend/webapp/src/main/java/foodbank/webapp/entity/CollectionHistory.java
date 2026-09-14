package foodbank.webapp.entity;

import java.time.LocalDateTime;

public class CollectionHistory {
    private Integer historyId;
    private Integer requestId;
    private Integer listingId;
    private Integer donorId;
    private Integer recipientId;
    private String category;
    private Float quantityCollected;
    private LocalDateTime collectedDatetime;
    private String pickupLocation;

    public Integer getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Integer historyId) {
        this.historyId = historyId;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public Integer getListingId() {
        return listingId;
    }

    public void setListingId(Integer listingId) {
        this.listingId = listingId;
    }

    public Integer getDonorId() {
        return donorId;
    }

    public void setDonorId(Integer donorId) {
        this.donorId = donorId;
    }

    public Integer getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Integer recipientId) {
        this.recipientId = recipientId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Float getQuantityCollected() {
        return quantityCollected;
    }

    public void setQuantityCollected(Float quantityCollected) {
        this.quantityCollected = quantityCollected;
    }

    public LocalDateTime getCollectedDatetime() {
        return collectedDatetime;
    }

    public void setCollectedDatetime(LocalDateTime collectedDatetime) {
        this.collectedDatetime = collectedDatetime;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }
}
