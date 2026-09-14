package foodbank.webapp.entity;

public class Request {
    private Integer requestId;
    private Integer timeslotId;
    private Integer recipientId;
    private Float quantityRequested;
    private String status;
    private String pickupCode;

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public Integer getTimeslotId() {
        return timeslotId;
    }

    public void setTimeslotId(Integer timeslotId) {
        this.timeslotId = timeslotId;
    }

    public Integer getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Integer recipientId) {
        this.recipientId = recipientId;
    }

    public Float getQuantityRequested() {
        return quantityRequested;
    }

    public void setQuantityRequested(Float quantityRequested) {
        this.quantityRequested = quantityRequested;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPickupCode() {
        return pickupCode;
    }

    public void setPickupCode(String pickupCode) {
        this.pickupCode = pickupCode;
    }
}
