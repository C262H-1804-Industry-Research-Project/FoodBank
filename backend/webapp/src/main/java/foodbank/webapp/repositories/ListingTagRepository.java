package foodbank.webapp.repositories;

import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.ListingTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingTagRepository extends JpaRepository<ListingTag, Integer> {
    List<ListingTag> findByListing(Listing listing);
}
