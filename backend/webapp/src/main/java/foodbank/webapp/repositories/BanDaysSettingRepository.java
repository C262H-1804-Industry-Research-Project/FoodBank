package foodbank.webapp.repositories;

import foodbank.webapp.entity.BanDaysSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BanDaysSettingRepository
        extends JpaRepository<BanDaysSetting, Integer> {
}