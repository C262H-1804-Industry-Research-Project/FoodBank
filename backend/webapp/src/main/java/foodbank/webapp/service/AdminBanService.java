package foodbank.webapp.service;

import foodbank.webapp.dto.BanUserRequest;
import foodbank.webapp.dto.BanUserResponse;
import foodbank.webapp.dto.UnbanUserResponse;
import foodbank.webapp.entity.BanRecord;
import foodbank.webapp.entity.Report;
import foodbank.webapp.entity.User;
import foodbank.webapp.exceptions.ConflictException;
import foodbank.webapp.exceptions.ForbiddenException;
import foodbank.webapp.repositories.AdminRepository;
import foodbank.webapp.repositories.BanRecordRepository;
import foodbank.webapp.repositories.ReportRepository;
import foodbank.webapp.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminBanService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final ReportRepository reportRepository;
    private final BanRecordRepository banRecordRepository;
    private final AuthTokenService authTokenService;

    @Transactional
    public BanUserResponse banUser(
            Integer userId,
            BanUserRequest request,
            Integer adminId
    ) {
        validateRequest(request, adminId);

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "User does not exist"
                        )
                );

        if ("BANNED".equalsIgnoreCase(user.getStatus())) {
            throw new ConflictException(
                    "User is already banned"
            );
        }

        if (!adminRepository.existsById(adminId)) {
            throw new ForbiddenException(
                    "Admin account does not exist"
            );
        }

        String banType = request.banType()
                .trim()
                .toUpperCase(Locale.ROOT);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = validateDates(
                banType,
                request.endDate(),
                startDate
        );

        validateReport(
                request.reportId(),
                userId
        );

        int banCount = Math.toIntExact(
                banRecordRepository.countByUserId(userId) + 1
        );
        String previousStatus = user.getStatus();

        BanRecord banRecord = banRecordRepository.save(
                BanRecord.builder()
                        .userId(userId)
                        .adminId(adminId)
                        .reportId(request.reportId())
                        .reason(request.reason().trim())
                        .banType(banType)
                        .banCount(banCount)
                        .startDate(startDate)
                        .endDate(endDate)
                        .previousStatus(previousStatus)
                        .active(Boolean.TRUE)
                        .build()

        );

        user.setStatus("BANNED");
        userRepository.save(user);

        // Immediately invalidate existing Donor/Recipient JWTs.
        authTokenService.revokeTokensForUser(userId);

        return new BanUserResponse(
                "User banned successfully",
                banRecord.getBanId(),
                user.getUserId(),
                adminId,
                banRecord.getReportId(),
                banRecord.getReason(),
                banRecord.getBanType(),
                banRecord.getBanCount(),
                banRecord.getStartDate(),
                banRecord.getEndDate(),
                user.getStatus()
        );
    }

    @Transactional
    public UnbanUserResponse unbanUser(Integer userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "User does not exist"
                        )
                );

        if (!"BANNED".equalsIgnoreCase(user.getStatus())) {
            throw new ConflictException(
                    "User is not banned"
            );
        }

        // Restore the status from before the first active ban, then close every active ban
        // so the ban expiry job does not touch this user again.
        List<BanRecord> activeBans =
                banRecordRepository.findByUserIdAndActiveTrueOrderByStartDateAsc(userId);

        String restoredStatus = activeBans.stream()
                .map(BanRecord::getPreviousStatus)
                .filter(status -> status != null
                        && !status.isBlank()
                        && !"BANNED".equalsIgnoreCase(status))
                .findFirst()
                .orElse("ACTIVE");

        for (BanRecord banRecord : activeBans) {
            banRecord.setActive(Boolean.FALSE);
        }
        banRecordRepository.saveAll(activeBans);

        user.setStatus(restoredStatus);
        userRepository.save(user);

        return new UnbanUserResponse(
                "User unbanned successfully",
                user.getUserId(),
                user.getStatus()
        );
    }

    private void validateReport(
            Integer reportId,
            Integer userId
    ) {
        if (reportId == null) {
            return;
        }

        Report report = reportRepository
                .findById(reportId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Report does not exist"
                        )
                );

        if (!Objects.equals(
                report.getReportedId(),
                userId
        )) {
            throw new ForbiddenException(
                    "Report does not belong to this user"
            );
        }
    }

    private static void validateRequest(
            BanUserRequest request,
            Integer adminId
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Request body is required"
            );
        }

        if (adminId == null) {
            throw new IllegalArgumentException(
                    "Authenticated admin is required"
            );
        }

        if (request.reason() == null
                || request.reason().isBlank()) {
            throw new IllegalArgumentException(
                    "reason is required"
            );
        }

        if (request.banType() == null
                || request.banType().isBlank()) {
            throw new IllegalArgumentException(
                    "banType is required"
            );
        }

        String banType = request.banType()
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!Set.of(
                "TEMPORARY",
                "PERMANENT"
        ).contains(banType)) {
            throw new IllegalArgumentException(
                    "banType must be TEMPORARY or PERMANENT"
            );
        }
    }

    private static LocalDate validateDates(
            String banType,
            LocalDate requestedEndDate,
            LocalDate startDate
    ) {
        if (banType.equals("TEMPORARY")) {
            if (requestedEndDate == null) {
                throw new IllegalArgumentException(
                        "endDate is required for a temporary ban"
                );
            }

            if (!requestedEndDate.isAfter(startDate)) {
                throw new IllegalArgumentException(
                        "endDate must be after startDate"
                );
            }

            return requestedEndDate;
        }

        if (requestedEndDate != null) {
            throw new IllegalArgumentException(
                    "endDate must be empty for a permanent ban"
            );
        }

        return null;
    }
}