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

    public List<AdminReportResponse> getAllReports() {
        return reportRepository
                .findAll()
                .stream()
                .map(AdminReportService::toResponse)
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

    private static AdminReportResponse toResponse(
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
                report.getStatus(),
                report.getAdminNote(),
                report.getOutcome(),
                report.getProcessedBy(),
                report.getProcessedAt(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
    private static String blankToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}