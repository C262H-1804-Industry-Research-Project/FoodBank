package foodbank.webapp.repositories;

import foodbank.webapp.dto.DonorReportResponse;
import foodbank.webapp.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Integer> {

    long countByReportedId(Integer reportedId);

    // Each row is [reportedId, number of reports against that user]
    @Query("SELECT r.reportedId, COUNT(r) FROM Report r WHERE r.reportedId IS NOT NULL GROUP BY r.reportedId")
    List<Object[]> countReportsPerReportedUser();

    // One query per page of the donor's report list. Report stores plain IDs, so these are
    // ad-hoc entity joins; LEFT JOIN keeps reports whose listing or request was deleted.
    // status and search are optional; search is a lower-case LIKE pattern matched against
    // the listing name, recipient name, reason and details.
    @Query(value = """
            SELECT new foodbank.webapp.dto.DonorReportResponse(
                r.reportId, r.reportedId, r.listingId, r.requestId, r.reportType,
                r.reason, r.details, r.status, r.outcome,
                r.processedAt, r.createdAt, r.updatedAt,
                l.name, rc.name, t.startTime, t.endTime,
                r.evidenceImageUrl, r.actualTransactionTime)
            FROM Report r
            LEFT JOIN Listing l ON l.listingId = r.listingId
            LEFT JOIN Recipient rc ON rc.userId = r.reportedId
            LEFT JOIN Request rq ON rq.requestId = r.requestId
            LEFT JOIN Timeslot t ON t.timeslotId = rq.timeslotId
            WHERE r.reporterId = :reporterId
              AND (:status IS NULL OR r.status = :status)
              AND (:search IS NULL
                   OR LOWER(l.name) LIKE :search ESCAPE '\\'
                   OR LOWER(rc.name) LIKE :search ESCAPE '\\'
                   OR LOWER(r.reason) LIKE :search ESCAPE '\\'
                   OR LOWER(r.details) LIKE :search ESCAPE '\\')
            ORDER BY r.createdAt DESC, r.reportId DESC
            """,
            countQuery = """
            SELECT COUNT(r)
            FROM Report r
            LEFT JOIN Listing l ON l.listingId = r.listingId
            LEFT JOIN Recipient rc ON rc.userId = r.reportedId
            WHERE r.reporterId = :reporterId
              AND (:status IS NULL OR r.status = :status)
              AND (:search IS NULL
                   OR LOWER(l.name) LIKE :search ESCAPE '\\'
                   OR LOWER(rc.name) LIKE :search ESCAPE '\\'
                   OR LOWER(r.reason) LIKE :search ESCAPE '\\'
                   OR LOWER(r.details) LIKE :search ESCAPE '\\')
            """)
    Page<DonorReportResponse> findDonorReports(
            @Param("reporterId") Integer reporterId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );

}
