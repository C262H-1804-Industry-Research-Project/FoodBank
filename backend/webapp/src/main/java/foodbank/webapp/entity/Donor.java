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
public class Donor {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer userId;
    private String orgName;
    private String addressLine;
    private String suburb;
    private String region;
    private String approvalStatus;
    private Boolean hideContact;
}
