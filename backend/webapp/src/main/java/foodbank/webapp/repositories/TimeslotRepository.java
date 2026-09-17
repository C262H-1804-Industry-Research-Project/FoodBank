package foodbank.webapp.repositories;

import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.Timeslot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeslotRepository extends JpaRepository<Timeslot, Integer> {
    List<Timeslot> findByListing(Listing listing);
}
