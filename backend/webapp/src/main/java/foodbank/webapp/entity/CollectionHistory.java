package foodbank.webapp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class CollectionHistory {

    @Id
    @GeneratedValue
    private Integer historyId;
    private Integer requestId;
    private Integer listingId;
    private Integer donorId;
    private Integer recipientId;
    private String category;
    private Float quantityCollected;
    private LocalDateTime collectedDatetime;
    private String pickupLocation;}
