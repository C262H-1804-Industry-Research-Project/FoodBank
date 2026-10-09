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
public class Recipient {

    @Id
    @GeneratedValue
    private Integer recipientId;
    private Integer userId;
    private String name;
    private String orgName;
    private String approvalStatus;
    private Integer claimLimit;

    // Location the recipient entered on the browse page, used to show distance to listings
    private String addressLine;
    private String suburb;
    private String region;
    private Double latitude;
    private Double longitude;
}
