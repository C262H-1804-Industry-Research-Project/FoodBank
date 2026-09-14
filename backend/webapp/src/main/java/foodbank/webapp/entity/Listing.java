package foodbank.webapp.entity;

import java.time.LocalDate;

public class Listing {
    private Integer listingId;
    private Integer donorId;
    private String category;
    private Float quantity;
    private LocalDate expiryDate;
    private LocalDate collectionDeadline;
    private String pickupAddress;
    private String status;
    private Integer maxDailyCapacity;
    private String photoUrl;
    private String notes;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Float getQuantity() {
        return quantity;
    }

    public void setQuantity(Float quantity) {
        this.quantity = quantity;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public LocalDate getCollectionDeadline() {
        return collectionDeadline;
    }

    public void setCollectionDeadline(LocalDate collectionDeadline) {
        this.collectionDeadline = collectionDeadline;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getMaxDailyCapacity() {
        return maxDailyCapacity;
    }

    public void setMaxDailyCapacity(Integer maxDailyCapacity) {
        this.maxDailyCapacity = maxDailyCapacity;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
