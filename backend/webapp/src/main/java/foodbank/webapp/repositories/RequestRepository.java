package foodbank.webapp.repositories;

import foodbank.webapp.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Integer> {

    List<Request> findByTimeslotId(Integer timeslotId);

    @Query("SELECT r FROM Request r WHERE r.status = :status AND r.timeslotId IN " +
           "(SELECT t.timeslotId FROM Timeslot t WHERE t.listing.donor.id = :donorId)")
    List<Request> findPendingByDonor(@Param("donorId") Integer donorId, @Param("status") String status);
}