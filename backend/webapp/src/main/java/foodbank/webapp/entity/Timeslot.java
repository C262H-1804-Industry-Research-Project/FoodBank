package foodbank.webapp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Timeslot {
    @Id
    @GeneratedValue
    private Integer timeslotId;

    @ManyToOne
    private Listing listing;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer maxRecipients;
    private Integer remainingSlot;
}
