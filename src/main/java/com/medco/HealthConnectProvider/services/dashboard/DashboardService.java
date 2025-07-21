package com.medco.HealthConnectProvider.services.dashboard;

import com.medco.HealthConnectProvider.ui.response.dashboard.ComprehensiveDashboardResponse;
import com.medco.HealthConnectProvider.ui.response.dashboard.DashboardResponse;

public interface DashboardService {

    DashboardResponse generateDashboardReport();
}