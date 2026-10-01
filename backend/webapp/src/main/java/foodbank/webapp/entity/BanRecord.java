package foodbank.webapp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDate;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class BanRecord {

    @Id
    @GeneratedValue
    private Integer banId;
    private Integer userId;
    private Integer adminId;
    private Integer reportId;
    // Reason entered by the administrator.
    private String reason;
    // TEMPORARY or PERMANENT.
    private String banType;
    // Number of bans recorded for this user.
    private Integer banCount;
    private LocalDate startDate;
    private LocalDate endDate;
}
