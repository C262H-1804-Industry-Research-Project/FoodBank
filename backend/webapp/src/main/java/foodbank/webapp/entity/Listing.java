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

    public static final int MAX_DESCRIPTION_LENGTH = 2000;

    @Id
    @GeneratedValue
    private Integer listingId;

    @ManyToOne
    private Donor donor;


    private String name;
    @Column(length = MAX_DESCRIPTION_LENGTH)
    private String description;
    @ManyToOne
    private Category category;
    private Float quantity;
    private LocalDate expiryDate;
    private LocalDate collectionDeadline;
    private String addressLine;
    private String suburb;
    private String region;
    // Pickup point coordinates, looked up from the address when the listing is saved
    private Double latitude;
    private Double longitude;
    private String status;
    private Integer maxDailyCapacity;
    private String photoUrl;

    private Boolean isAutoApproved;
    private Boolean usePickupCode;
    private Boolean hideContact;
    private Integer slotCapacity;
}
