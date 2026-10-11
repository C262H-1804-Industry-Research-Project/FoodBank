package foodbank.webapp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class BanDaysSetting {

    /**
     * The platform uses one global settings row with ID 1.
     */
    @Id
    private Integer settingId;

    // Default temporary-ban duration for the first offence.
    private Integer firstOffenceDays;

    // Default temporary-ban duration for the second offence.
    private Integer secondOffenceDays;

    // Default temporary-ban duration for the third offence.
    private Integer thirdOffenceDays;
}