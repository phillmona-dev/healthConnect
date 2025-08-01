package com.medco.HealthConnectProvider.controller.dashboard;

import com.medco.HealthConnectProvider.services.dashboard.DashboardService;
import com.medco.HealthConnectProvider.ui.response.dashboard.DashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/healthConnect/dashboard")
@Tag(name = "Dashboard report", description = "Dashboard API for generating reports and statistics")
public class DashboardController {

    private final DashboardService dashboardService;

    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

//    @GetMapping("/report")
//    @Operation(
//            summary = "Generate Dashboard Report",
//            description = "Generates a comprehensive dashboard report including total counts of payers, providers, " +
//                    "insured individuals, claims, and groups. It also provides summaries for payers and providers, " +
//                    "claim statistics across different statuses, and monthly claim totals."
//    )
//    public ResponseEntity<DashboardResponse> getDashboardReport() {
//        DashboardResponse report = dashboardService.generateDashboardReport();
//        return ResponseEntity.ok(report);
//    }

    @GetMapping("/report")
    @Operation(
            summary = "Generate Dashboard Report",
            description = "Generates a dashboard report based on the user's role. Admins see comprehensive data, while providers and payers see their own data."
    )
    public ResponseEntity<DashboardResponse> getDashboardReport() {
        DashboardResponse report = dashboardService.generateDashboardReport();
        return ResponseEntity.ok(report);
    }
}