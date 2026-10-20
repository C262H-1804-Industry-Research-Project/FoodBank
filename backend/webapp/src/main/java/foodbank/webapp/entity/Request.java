package foodbank.webapp.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import foodbank.webapp.service.RequestService;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Request {
    @Id
    @GeneratedValue
    private Integer requestId;
    private Integer timeslotId;
    private Integer recipientId;
    private Float quantityRequested;
    private String status;
    private String pickupCode;

    // Optional message from the recipient to the donor.
    @Column(length = RequestService.MAX_NOTE_LENGTH)
    private String note;

    // Why a REJECTED request was rejected: DECLINED_BY_DONOR or NOT_REVIEWED_IN_TIME
    private String rejectionReason;

    // When StockReleaseService stopped this EXPIRED request holding listing stock; null while it still holds it
    private LocalDateTime stockReleasedAt;
}
