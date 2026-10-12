package foodbank.webapp.service;

import foodbank.webapp.dto.ProcessReportRequest;
import foodbank.webapp.dto.AdminReportResponse;
import foodbank.webapp.entity.Report;
import foodbank.webapp.repositories.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Locale;
import java.util.Set;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final ReportRepository reportRepository;
    private final AdminBanService adminBanService;
    private final foodbank.webapp.repositories.DonorRepository donorRepository;
    private final foodbank.webapp.repositories.RecipientRepository recipientRepository;
    private final foodbank.webapp.repositories.ListingRepository listingRepository;
    private final foodbank.webapp.repositories.BanRecordRepository banRecordRepository;

    public List<AdminReportResponse> getAllReports() {
        return reportRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }
    public AdminReportResponse getReportById(
            Integer reportId
    ) {
        Report report = reportRepository
                .findById(reportId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Report does not exist"
                        )
                );

        return toResponse(report);
    }
    @org.springframework.transaction.annotation.Transactional
    public AdminReportResponse processReport(
            Integer reportId,
            ProcessReportRequest request,
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

        if (request.status() == null
                || request.status().isBlank()) {
            throw new IllegalArgumentException(
                    "status is required"
            );
        }

        String status = request.status()
                .trim()
                .toUpperCase(Locale.ROOT);

        Set<String> allowedStatuses = Set.of(
                "IN_REVIEW",
                "RESOLVED",
                "DISMISSED"
        );

        if (!allowedStatuses.contains(status)) {
            throw new IllegalArgumentException(
                    "status must be one of IN_REVIEW, RESOLVED, DISMISSED"
            );
        }

        Report report = reportRepository
                .findById(reportId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Report does not exist"
                        )
                );

        if (request.action() != null) {
            if (Set.of("RESOLVED", "DISMISSED").contains(report.getStatus())) {
                throw new foodbank.webapp.exceptions.ConflictException("Report has already been processed");
            }
            String action = request.action().trim().toUpperCase(Locale.ROOT);
            if (!Set.of("BAN", "DEACTIVATE", "WARNING", "DISMISS", "REVIEW").contains(action)) {
                throw new IllegalArgumentException("Invalid report action");
            }
            String expected = action.equals("DISMISS") ? "DISMISSED" : action.equals("REVIEW") ? "IN_REVIEW" : "RESOLVED";
            if (!expected.equals(status)) throw new IllegalArgumentException("Action and status do not match");
            if (action.equals("BAN") || action.equals("DEACTIVATE")) {
                java.time.LocalDate end = null;
                if (action.equals("BAN")) {
                    if (request.banDays() == null || request.banDays() < 1 || request.banDays() > 365)
                        throw new IllegalArgumentException("banDays must be between 1 and 365");
                    end = java.time.LocalDate.now().plusDays(request.banDays());
                }
                adminBanService.banUser(report.getReportedId(), new foodbank.webapp.dto.BanUserRequest(
                        reportId, blankToNull(request.adminNote()) == null ? report.getReason() : request.adminNote(),
                        action.equals("BAN") ? "TEMPORARY" : "PERMANENT", end), adminId);
            }
        }
        report.setStatus(status);
        report.setAdminNote(
                blankToNull(request.adminNote())
        );
        report.setOutcome(
                blankToNull(request.outcome())
        );
        report.setProcessedBy(adminId);
        report.setProcessedAt(LocalDateTime.now());

        Report updatedReport =
                reportRepository.save(report);

        return toResponse(updatedReport);
    }

    private AdminReportResponse toResponse(
            Report report
    ) {
        return new AdminReportResponse(
                report.getReportId(),
                report.getReporterId(),
                report.getReportedId(),
                report.getListingId(),
                report.getRequestId(),
                report.getReportType(),
                report.getReason(),
                report.getDetails(),
                report.getEvidenceImageUrl(),
                report.getActualTransactionTime(),
                report.getStatus(),
                report.getAdminNote(),
                report.getOutcome(),
                report.getProcessedBy(),
                report.getProcessedAt(),
                report.getCreatedAt(),
                report.getUpdatedAt(),
                userName(report.getReporterId()),
                userName(report.getReportedId()),
                donorRepository.existsByUserId(report.getReportedId()) ? "Donor" : "Recipient",
                report.getListingId() == null ? null : listingRepository.findById(report.getListingId()).map(foodbank.webapp.entity.Listing::getName).orElse(null),
                banRecordRepository.countByUserId(report.getReportedId())
        );
    }
    private String userName(Integer userId) {
        return donorRepository.findByUserId(userId).map(foodbank.webapp.entity.Donor::getOrgName)
                .orElseGet(() -> recipientRepository.findByUserId(userId).map(foodbank.webapp.entity.Recipient::getName).orElse("User #" + userId));
    }
    private static String blankToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}