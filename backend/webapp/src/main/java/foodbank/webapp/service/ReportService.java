package foodbank.webapp.service;

import foodbank.webapp.dto.CreateReportRequest;
import foodbank.webapp.dto.CreatedReportResponse;
import foodbank.webapp.dto.DonorReportResponse;
import foodbank.webapp.dto.RecipientReportResponse;
import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.entity.Report;
import foodbank.webapp.entity.Request;
import foodbank.webapp.entity.Timeslot;
import foodbank.webapp.exceptions.ConflictException;
import foodbank.webapp.exceptions.ForbiddenException;
import foodbank.webapp.repositories.DonorRepository;
import foodbank.webapp.repositories.ListingRepository;
import foodbank.webapp.repositories.RecipientRepository;
import foodbank.webapp.repositories.ReportRepository;
import foodbank.webapp.repositories.RequestRepository;
import foodbank.webapp.repositories.TimeslotRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ListingRepository listingRepository;
    private final RequestRepository requestRepository;
    private final TimeslotRepository timeslotRepository;
    private final DonorRepository donorRepository;
    private final RecipientRepository recipientRepository;

    // status and search are optional; search matches the listing name,
    // recipient name, reason or details, ignoring case.
    public Page<DonorReportResponse> getReportsByDonor(
            Integer donorId,
            String status,
            String search,
            Pageable pageable
    ) {
        String statusFilter = statusFilter(status);

        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() ->
                        new NoSuchElementException("Donor not found")
                );

        String searchPattern = searchPattern(search);

        return reportRepository.findDonorReports(
                donor.getUserId(), statusFilter, searchPattern, pageable
        );
    }

    // status and search are optional; search matches the listing name,
    // donor name, reason or details, ignoring case.
    public Page<RecipientReportResponse> getReportsByRecipient(
            Integer userId,
            String status,
            String search,
            Pageable pageable
    ) {
        String statusFilter = statusFilter(status);

        recipientRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ForbiddenException("Recipient profile does not exist")
                );

        return reportRepository.findRecipientReports(
                userId, statusFilter, searchPattern(search), pageable
        );
    }

    private static String statusFilter(String status) {
        String statusFilter = blankToNull(status);
        if (statusFilter == null) {
            return null;
        }
        statusFilter = statusFilter.toUpperCase(Locale.ROOT);
        if (!Report.STATUSES.contains(statusFilter)) {
            throw new IllegalArgumentException(
                    "status must be one of " + String.join(", ", Report.STATUSES)
            );
        }
        return statusFilter;
    }

    private static String searchPattern(String search) {
        String searchTerm = blankToNull(search);
        // Escape LIKE wildcards so the search matches the text literally
        return searchTerm == null ? null
                : "%" + searchTerm.toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_") + "%";
    }

    @Transactional
    public CreatedReportResponse submitReport(
            CreateReportRequest request,
            Integer reporterUserId,
            String reporterRole
    ) {
        validateBasicFields(
                request,
                reporterUserId,
                reporterRole
        );

        String role = reporterRole
                .trim()
                .toLowerCase(Locale.ROOT);

        String reportType = request.reportType()
                .trim()
                .toUpperCase(Locale.ROOT);

        Listing listing = listingRepository
                .findById(request.listingId())
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Listing does not exist"
                        )
                );

        Integer reportedUserId;
        Integer pickupRequestId;

        if (role.equals("recipient")) {
            if (!reportType.equals("FOOD_QUALITY")) {
                throw new ForbiddenException(
                        "Recipient may submit only FOOD_QUALITY reports"
                );
            }

            Recipient reporter = recipientRepository
                    .findByUserId(reporterUserId)
                    .orElseThrow(() ->
                            new ForbiddenException(
                                    "Recipient profile does not exist"
                            )
                    );

            Request pickupRequest =
                    findAndValidatePickupRequest(
                            request.pickupRequestId(),
                            request.listingId()
                    );

            if (!Objects.equals(
                    pickupRequest.getRecipientId(),
                    reporter.getRecipientId()
            )) {
                throw new ForbiddenException(
                        "Recipient cannot report this pickup request"
                );
            }

            if (!RequestService.REPORTABLE_STATUSES.contains(pickupRequest.getStatus())) {
                throw new ConflictException(
                        "Only approved or completed pickup requests can be reported"
                );
            }

            if (listing.getDonor() == null) {
                throw new NoSuchElementException(
                        "Listing donor does not exist"
                );
            }

            reportedUserId =
                    listing.getDonor().getUserId();

            pickupRequestId =
                    pickupRequest.getRequestId();

        } else if (role.equals("donor")) {
            if (!reportType.equals(
                    "RECIPIENT_BEHAVIOUR"
            )) {
                throw new ForbiddenException(
                        "Donor may submit only RECIPIENT_BEHAVIOUR reports"
                );
            }

            Donor reporter = donorRepository
                    .findByUserId(reporterUserId)
                    .orElseThrow(() ->
                            new ForbiddenException(
                                    "Donor profile does not exist"
                            )
                    );

            if (listing.getDonor() == null
                    || !Objects.equals(
                    listing.getDonor().getId(),
                    reporter.getId()
            )) {
                throw new ForbiddenException(
                        "Donor does not own this listing"
                );
            }

            if (request.reportedUserId() == null) {
                throw new IllegalArgumentException(
                        "reportedUserId is required"
                );
            }

            Request pickupRequest =
                    findAndValidatePickupRequest(
                            request.pickupRequestId(),
                            request.listingId()
                    );

            Recipient reportedRecipient =
                    recipientRepository
                            .findById(
                                    pickupRequest.getRecipientId()
                            )
                            .orElseThrow(() ->
                                    new NoSuchElementException(
                                            "Reported recipient does not exist"
                                    )
                            );

            if (!Objects.equals(
                    reportedRecipient.getUserId(),
                    request.reportedUserId()
            )) {
                throw new ForbiddenException(
                        "Reported user does not match the pickup request"
                );
            }

            reportedUserId =
                    reportedRecipient.getUserId();

            pickupRequestId =
                    pickupRequest.getRequestId();

        } else {
            throw new ForbiddenException(
                    "Only Donor or Recipient may submit reports"
            );
        }

        Report report = reportRepository.save(
                Report.builder()
                        .reporterId(reporterUserId)
                        .reportedId(reportedUserId)
                        .listingId(listing.getListingId())
                        .requestId(pickupRequestId)
                        .reportType(reportType)
                        .reason(request.reason().trim())
                        .details(
                                blankToNull(request.details())
                        )
                        .evidenceImageUrl(
                                blankToNull(request.evidenceImageUrl())
                        )
                        .actualTransactionTime(
                                request.actualTransactionTime()
                        )
                        .build()
        );

        return new CreatedReportResponse(
                "Report submitted successfully",
                report.getReportId(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }

    private Request findAndValidatePickupRequest(
            Integer pickupRequestId,
            Integer listingId
    ) {
        if (pickupRequestId == null) {
            throw new IllegalArgumentException(
                    "pickupRequestId is required"
            );
        }

        Request pickupRequest = requestRepository
                .findById(pickupRequestId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Pickup request does not exist"
                        )
                );

        Timeslot timeslot = timeslotRepository
                .findById(pickupRequest.getTimeslotId())
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Pickup timeslot does not exist"
                        )
                );

        if (timeslot.getListing() == null
                || !Objects.equals(
                timeslot.getListing().getListingId(),
                listingId
        )) {
            throw new ForbiddenException(
                    "Pickup request does not belong to this listing"
            );
        }

        return pickupRequest;
    }

    private static void validateBasicFields(
            CreateReportRequest request,
            Integer reporterUserId,
            String reporterRole
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Request body is required"
            );
        }

        if (reporterUserId == null) {
            throw new IllegalArgumentException(
                    "Authenticated user is required"
            );
        }

        requireText(
                reporterRole,
                "reporterRole"
        );

        requireText(
                request.reportType(),
                "reportType"
        );

        if (request.listingId() == null) {
            throw new IllegalArgumentException(
                    "listingId is required"
            );
        }

        requireText(
                request.reason(),
                "reason"
        );
    }

    private static void requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " is required"
            );
        }
    }

    private static String blankToNull(
            String value
    ) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}