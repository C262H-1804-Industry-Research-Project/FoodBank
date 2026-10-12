package foodbank.webapp.repositories;

import foodbank.webapp.dto.DonorReportResponse;
import foodbank.webapp.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Integer> {

    long countByReportedId(Integer reportedId);

    // One query for the donor's report list. Report stores plain IDs, so these are
    // ad-hoc entity joins; LEFT JOIN keeps reports whose listing or request was deleted.
    @Query("""
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
            ORDER BY r.createdAt DESC
            """)
    List<DonorReportResponse> findDonorReports(@Param("reporterId") Integer reporterId);

}
