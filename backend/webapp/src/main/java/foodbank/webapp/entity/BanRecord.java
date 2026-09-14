package foodbank.webapp.entity;

import java.time.LocalDate;

public class BanRecord {
    private Integer banId;
    private Integer userId;
    private Integer adminId;
    private Integer reportId;
    private Integer banCount;
    private LocalDate startDate;
    private LocalDate endDate;

    public Integer getBanId() {
        return banId;
    }

    public void setBanId(Integer banId) {
        this.banId = banId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }

    public Integer getReportId() {
        return reportId;
    }

    public void setReportId(Integer reportId) {
        this.reportId = reportId;
    }

    public Integer getBanCount() {
        return banCount;
    }

    public void setBanCount(Integer banCount) {
        this.banCount = banCount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
