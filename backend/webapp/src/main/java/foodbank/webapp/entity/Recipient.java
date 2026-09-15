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
}
