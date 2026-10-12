package foodbank.webapp.service;

import foodbank.webapp.dto.BanDaysResponse;
import foodbank.webapp.dto.UpdateBanDaysRequest;
import foodbank.webapp.entity.BanDaysSetting;
import foodbank.webapp.repositories.BanDaysSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BanDaysSettingService {

    private static final Integer GLOBAL_SETTING_ID = 1;

    private static final int DEFAULT_FIRST_DAYS = 3;
    private static final int DEFAULT_SECOND_DAYS = 7;
    private static final int DEFAULT_THIRD_DAYS = 30;

    private final BanDaysSettingRepository banDaysSettingRepository;

    /**
     * Returns the global ban-duration policy.
     * The default policy is created when no database row exists yet.
     */
    @Transactional
    public BanDaysResponse getSettings() {
        BanDaysSetting setting = getOrCreateSetting();
        return toResponse(setting);
    }

    /**
     * Updates the global ban-duration policy.
     */
    @Transactional
    public BanDaysResponse updateSettings(
            UpdateBanDaysRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Request body is required"
            );
        }

        validateDays(request.first(), "first");
        validateDays(request.second(), "second");
        validateDays(request.third(), "third");

        BanDaysSetting setting = getOrCreateSetting();

        setting.setFirstOffenceDays(request.first());
        setting.setSecondOffenceDays(request.second());
        setting.setThirdOffenceDays(request.third());

        BanDaysSetting saved =
                banDaysSettingRepository.save(setting);

        return toResponse(saved);
    }

    private BanDaysSetting getOrCreateSetting() {
        return banDaysSettingRepository
                .findById(GLOBAL_SETTING_ID)
                .orElseGet(() ->
                        banDaysSettingRepository.save(
                                BanDaysSetting.builder()
                                        .settingId(
                                                GLOBAL_SETTING_ID
                                        )
                                        .firstOffenceDays(
                                                DEFAULT_FIRST_DAYS
                                        )
                                        .secondOffenceDays(
                                                DEFAULT_SECOND_DAYS
                                        )
                                        .thirdOffenceDays(
                                                DEFAULT_THIRD_DAYS
                                        )
                                        .build()
                        )
                );
    }

    private static void validateDays(
            Integer days,
            String field
    ) {
        if (days == null) {
            throw new IllegalArgumentException(
                    field + " is required"
            );
        }

        if (days < 1 || days > 365) {
            throw new IllegalArgumentException(
                    field + " must be between 1 and 365"
            );
        }
    }

    private static BanDaysResponse toResponse(
            BanDaysSetting setting
    ) {
        return new BanDaysResponse(
                setting.getFirstOffenceDays(),
                setting.getSecondOffenceDays(),
                setting.getThirdOffenceDays()
        );
    }
}