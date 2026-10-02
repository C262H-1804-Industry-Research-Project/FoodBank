package foodbank.webapp.controllers;
import foodbank.webapp.dto.ProcessReportRequest;
import foodbank.webapp.dto.AdminReportResponse;
import foodbank.webapp.service.AdminReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping
    public List<AdminReportResponse> getAllReports() {
        return adminReportService.getAllReports();
    }
    @GetMapping("/{reportId}")
    public AdminReportResponse getReportById(
            @PathVariable Integer reportId
    ) {
        return adminReportService.getReportById(reportId);
    }
    @PatchMapping("/{reportId}")
    public AdminReportResponse processReport(
            @PathVariable Integer reportId,
            @RequestBody ProcessReportRequest request,
            Authentication authentication
    ) {
        Integer adminId =
                Integer.valueOf(authentication.getName());

        return adminReportService.processReport(
                reportId,
                request,
                adminId
        );
    }
}