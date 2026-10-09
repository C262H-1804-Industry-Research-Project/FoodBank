package foodbank.webapp.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import foodbank.webapp.service.RequestService;
import lombok.*;

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
}
