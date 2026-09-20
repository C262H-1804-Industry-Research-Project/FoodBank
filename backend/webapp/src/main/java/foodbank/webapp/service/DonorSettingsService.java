package foodbank.webapp.service;

import foodbank.webapp.dto.*;
import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Report;
import foodbank.webapp.repositories.DonorRepository;
import foodbank.webapp.repositories.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class DonorSettingsService {
    private final DonorRepository donorRepository;
    private final ReportRepository reportRepository;

    public DonorSettingsResponse getSettings(Integer donorId) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new NoSuchElementException("Donor not found"));
        return new DonorSettingsResponse(donor.getId(), donor.getHideContact());
    }

    @Transactional
    public DonorSettingsResponse updateSettings(Integer donorId, UpdateDonorSettingsRequest request) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new NoSuchElementException("Donor not found"));
        donor.setHideContact(request.hideContact());
        donorRepository.save(donor);
        return new DonorSettingsResponse(donor.getId(), donor.getHideContact());
    }

    @Transactional
    public ReportResponse createReport(CreateReportRequest request) {
        if (request.reporterId() == null || request.reportedId() == null) {
            throw new IllegalArgumentException("reporterId and reportedId are required");
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }

        Report report = reportRepository.save(Report.builder()
                .reporterId(request.reporterId())
                .reportedId(request.reportedId())
                .listingId(request.listingId())
                .requestId(request.requestId())
                .reason(request.reason().trim())
                .status("PENDING")
                .build());

        return new ReportResponse(report.getReportId(), report.getStatus());
    }
}
