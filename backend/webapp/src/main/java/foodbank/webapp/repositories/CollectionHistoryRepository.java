package foodbank.webapp.repositories;

import foodbank.webapp.entity.CollectionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollectionHistoryRepository extends JpaRepository<CollectionHistory, Integer> {
    List<CollectionHistory> findByDonorId(Integer donorId);

    long countByDonorId(Integer donorId);

    long countByRecipientId(Integer recipientId);

}