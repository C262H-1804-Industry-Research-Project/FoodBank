package foodbank.webapp.repositories;

import foodbank.webapp.entity.CollectionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CollectionHistoryRepository extends JpaRepository<CollectionHistory, Integer> {
    List<CollectionHistory> findByDonorIdOrderByCollectedDatetimeDescHistoryIdDesc(Integer donorId);

    long countByDonorId(Integer donorId);

    long countByRecipientId(Integer recipientId);

    @Query("SELECT COUNT(h) FROM CollectionHistory h WHERE h.donorId = :donorId " +
            "AND (h.status IS NULL OR TRIM(h.status) = '' OR UPPER(h.status) = 'COMPLETED')")
    long countSuccessfulByDonorId(@Param("donorId") Integer donorId);

    @Query("SELECT COUNT(h) FROM CollectionHistory h WHERE h.recipientId = :recipientId " +
            "AND (h.status IS NULL OR TRIM(h.status) = '' OR UPPER(h.status) = 'COMPLETED')")
    long countSuccessfulByRecipientId(@Param("recipientId") Integer recipientId);

}
