package foodbank.webapp.entity;

public class Donor {
    private Integer donorId;
    private Integer userId;
    private String orgName;
    private String location;
    private String approvalStatus;
    private Boolean hideContact;

    public Integer getDonorId() {
        return donorId;
    }

    public void setDonorId(Integer donorId) {
        this.donorId = donorId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public Boolean getHideContact() {
        return hideContact;
    }

    public void setHideContact(Boolean hideContact) {
        this.hideContact = hideContact;
    }
}
