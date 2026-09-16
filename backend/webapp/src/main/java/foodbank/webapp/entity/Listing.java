package foodbank.webapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;


@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Listing {

    @Id
    @GeneratedValue
    private Integer listingId;

    @ManyToOne
    private Donor donor;


    private String name;
    private String description;
    @ManyToOne
    private Category category;
    private Float quantity;
    private LocalDate expiryDate;
    private LocalDate collectionDeadline;
    private String addressLine;
    private String suburb;
    private String region;
    private String status;
    private Integer maxDailyCapacity;
    private String photoUrl;
    private String notes;
}
