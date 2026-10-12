package foodbank.webapp.service;

import foodbank.webapp.entity.BanRecord;
import foodbank.webapp.entity.User;
import foodbank.webapp.repositories.BanRecordRepository;
import foodbank.webapp.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BanExpiryService {

    private final BanRecordRepository banRecordRepository;
    private final UserRepository userRepository;

    /**
     * Checks expired temporary bans once every minute.
     */
    @Scheduled(fixedDelayString = "${BAN_EXPIRY_CHECK_MS:60000}")
    @Transactional
    public void releaseExpiredTemporaryBans() {

        List<BanRecord> expiredBans =
                banRecordRepository
                        .findByBanTypeIgnoreCaseAndActiveTrueAndEndDateLessThanEqual(
                                "TEMPORARY",
                                LocalDate.now()
                        );

        for (BanRecord banRecord : expiredBans) {
            User user = userRepository
                    .findById(banRecord.getUserId())
                    .orElse(null);

            if (user != null &&
                    "BANNED".equalsIgnoreCase(user.getStatus())) {

                String restoredStatus = banRecord.getPreviousStatus();

                if (restoredStatus == null || restoredStatus.isBlank()) {
                    restoredStatus = "ACTIVE";
                }

                user.setStatus(restoredStatus);
                userRepository.save(user);
            }

            banRecord.setActive(Boolean.FALSE);
            banRecordRepository.save(banRecord);
        }
    }
}