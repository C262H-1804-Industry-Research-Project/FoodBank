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

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Report {

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

    // PENDING, IN_REVIEW, RESOLVED or DISMISSED.
    private String status;

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