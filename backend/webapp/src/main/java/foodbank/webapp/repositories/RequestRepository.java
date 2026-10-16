package foodbank.webapp.repositories;

import foodbank.webapp.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Integer> {

    List<Request> findByTimeslotId(Integer timeslotId);

    List<Request> findByRecipientId(Integer recipientId);

    List<Request> findByRecipientIdAndStatusIn(Integer recipientId, Collection<String> statuses);

    long countByRecipientId(Integer recipientId);

    boolean existsByTimeslotIdAndRecipientIdAndStatusNot(
            Integer timeslotId, Integer recipientId, String status);

    @Query("SELECT r FROM Request r WHERE r.status IN :statuses AND r.timeslotId IN " +
           "(SELECT t.timeslotId FROM Timeslot t WHERE t.listing.donor.id = :donorId)")
    List<Request> findByDonorAndStatusIn(@Param("donorId") Integer donorId,
                                         @Param("statuses") Collection<String> statuses);

    @Query("SELECT r FROM Request r WHERE r.timeslotId IN " +
           "(SELECT t.timeslotId FROM Timeslot t WHERE t.listing.donor.id = :donorId)")
    List<Request> findAllByDonor(@Param("donorId") Integer donorId);

    @Query("SELECT SUM(r.quantityRequested) FROM Request r " +
           "WHERE r.recipientId = :recipientId AND r.status NOT IN ('REJECTED', 'CANCELLED') AND r.timeslotId IN " +
           "(SELECT t.timeslotId FROM Timeslot t WHERE t.listing.listingId = :listingId " +
           "AND t.startTime >= :dayStart AND t.startTime < :dayEnd)")
    Double sumActiveQuantityForListing(@Param("recipientId") Integer recipientId,
                                       @Param("listingId") Integer listingId,
                                       @Param("dayStart") LocalDateTime dayStart,
                                       @Param("dayEnd") LocalDateTime dayEnd);

    @Query("SELECT SUM(r.quantityRequested) FROM Request r " +
           "WHERE r.recipientId = :recipientId AND r.status NOT IN ('REJECTED', 'CANCELLED') AND r.timeslotId IN " +
           "(SELECT t.timeslotId FROM Timeslot t " +
           "WHERE t.startTime >= :dayStart AND t.startTime < :dayEnd)")
    Double sumActiveQuantityForDay(@Param("recipientId") Integer recipientId,
                                   @Param("dayStart") LocalDateTime dayStart,
                                   @Param("dayEnd") LocalDateTime dayEnd);
}