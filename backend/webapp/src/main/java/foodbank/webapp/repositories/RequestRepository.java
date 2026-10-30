package foodbank.webapp.repositories;

import foodbank.webapp.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Integer> {

    @Query("SELECT COUNT(r) FROM Request r WHERE r.timeslotId IN " +
            "(SELECT t.timeslotId FROM Timeslot t WHERE t.listing.donor.id = :donorId)")
    long countTotalByDonorId(@Param("donorId") Integer donorId);

    @Query("SELECT COUNT(r) FROM Request r WHERE r.timeslotId IN " +
            "(SELECT t.timeslotId FROM Timeslot t WHERE t.listing.donor.id = :donorId) " +
            "AND (r.status = 'COMPLETED' OR EXISTS (SELECT h.historyId FROM CollectionHistory h " +
            "WHERE h.requestId = r.requestId AND h.donorId = :donorId " +
            "AND (h.status IS NULL OR TRIM(h.status) = '' OR UPPER(h.status) = 'COMPLETED')))")
    long countCollectedByDonorId(@Param("donorId") Integer donorId);

    @Query("SELECT COUNT(r) FROM Request r WHERE r.recipientId = :recipientId " +
            "AND (r.status = 'COMPLETED' OR EXISTS (SELECT h.historyId FROM CollectionHistory h " +
            "WHERE h.requestId = r.requestId AND h.recipientId = :recipientId " +
            "AND (h.status IS NULL OR TRIM(h.status) = '' OR UPPER(h.status) = 'COMPLETED')))")
    long countCollectedByRecipientId(@Param("recipientId") Integer recipientId);

    // Approved applications remain in the denominator after collection or expiry.
    @Query("SELECT COUNT(r) FROM Request r WHERE r.status IN ('APPROVED', 'COMPLETED', 'EXPIRED', 'FAILED') " +
            "AND r.timeslotId IN (SELECT t.timeslotId FROM Timeslot t WHERE t.listing.listingId = :listingId)")
    long countApprovedForListing(@Param("listingId") Integer listingId);

    @Query("SELECT COUNT(r) FROM Request r WHERE r.status IN ('APPROVED', 'COMPLETED', 'EXPIRED', 'FAILED') " +
            "AND r.timeslotId IN (SELECT t.timeslotId FROM Timeslot t WHERE t.listing.listingId = :listingId) " +
            "AND (r.status = 'COMPLETED' OR EXISTS (SELECT h.historyId FROM CollectionHistory h " +
            "WHERE h.requestId = r.requestId AND h.listingId = :listingId " +
            "AND (h.status IS NULL OR TRIM(h.status) = '' OR UPPER(h.status) = 'COMPLETED')))")
    long countCompletedForListing(@Param("listingId") Integer listingId);

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

    @Query("SELECT r FROM Request r WHERE r.status IN :statuses AND r.timeslotId IN " +
           "(SELECT t.timeslotId FROM Timeslot t WHERE t.endTime <= :now)")
    List<Request> findSlotEndedByStatusIn(@Param("statuses") Collection<String> statuses,
                                          @Param("now") LocalDateTime now);

    // Released EXPIRED requests no longer hold stock
    @Query("SELECT SUM(r.quantityRequested) FROM Request r WHERE r.status IN :statuses " +
           "AND NOT (r.status = 'EXPIRED' AND r.stockReleasedAt IS NOT NULL) AND r.timeslotId IN " +
           "(SELECT t.timeslotId FROM Timeslot t WHERE t.listing.listingId = :listingId)")
    Double sumQuantityForListing(@Param("listingId") Integer listingId,
                                 @Param("statuses") Collection<String> statuses);

    @Query("SELECT r FROM Request r WHERE r.status = 'EXPIRED' AND r.stockReleasedAt IS NULL AND r.timeslotId IN " +
           "(SELECT t.timeslotId FROM Timeslot t WHERE t.endTime <= :cutoff)")
    List<Request> findUnreleasedExpiredWithSlotEndedBy(@Param("cutoff") LocalDateTime cutoff);

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
