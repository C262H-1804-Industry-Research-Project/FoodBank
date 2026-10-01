package foodbank.webapp.repositories;

import foodbank.webapp.entity.BanRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BanRecordRepository
        extends JpaRepository<BanRecord, Integer> {

    long countByUserId(Integer userId);
}