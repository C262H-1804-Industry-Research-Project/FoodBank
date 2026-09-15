package foodbank.webapp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ListingTag {

    @Id
    @GeneratedValue
    private Integer id;

    @ManyToOne
    private Listing listing;

    @ManyToOne
    private Tag tag;

}
