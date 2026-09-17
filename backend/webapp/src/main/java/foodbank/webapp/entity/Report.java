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
public class Report {

    @Id
    @GeneratedValue
    private Integer reportId;
    private Integer reporterId;
    private Integer reportedId;
    private Integer listingId;
    private Integer requestId;
    private String reason;
    private String status;

}
