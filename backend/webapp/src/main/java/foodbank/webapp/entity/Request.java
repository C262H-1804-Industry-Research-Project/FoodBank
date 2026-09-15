package foodbank.webapp.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
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
}
