package foodbank.webapp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Report {

    // Every status a report can have, in workflow order.
    public static final List<String> STATUSES =
            List.of("PENDING", "IN_REVIEW", "RESOLVED", "DISMISSED");

    @Id
    @GeneratedValue
    private Integer reportId;

    // The logged-in user who submitted the report.
    private Integer reporterId;

    // The user being reported.
    private Integer reportedId;

    // The listing involved in the report.
    private Integer listingId;

    // Corresponds to pickupRequestId in the API request.
    private Integer requestId;

    // FOOD_QUALITY or RECIPIENT_BEHAVIOUR.
    private String reportType;

    // Selected reason category from the frontend.
    private String reason;

    // Additional explanation entered by the user.
    private String details;

    // Optional evidence photo URL, uploaded via /api/uploads/photo.
    private String evidenceImageUrl;

    // When the pickup actually happened, as recorded by the reporter (optional).
    private LocalDateTime actualTransactionTime;

    // PENDING, IN_REVIEW, RESOLVED or DISMISSED.
    private String status;

    // Internal note written by the administrator.
    private String adminNote;

    // Final action or decision for the report.
    private String outcome;

    // The Admin ID that most recently processed the report.
    private Integer processedBy;

    // The time when an administrator processed the report.
    private LocalDateTime processedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void beforeCreate() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = "PENDING";
        }
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = LocalDateTime.now();
    }
}