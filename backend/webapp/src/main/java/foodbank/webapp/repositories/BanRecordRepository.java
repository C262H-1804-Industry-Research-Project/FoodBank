package foodbank.webapp.repositories;

import foodbank.webapp.entity.BanRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BanRecordRepository
        extends JpaRepository<BanRecord, Integer> {

    long countByUserId(Integer userId);

    List<BanRecord> findByBanTypeIgnoreCaseAndActiveTrueAndEndDateLessThanEqual(
            String banType,
            LocalDate endDate
    );
}