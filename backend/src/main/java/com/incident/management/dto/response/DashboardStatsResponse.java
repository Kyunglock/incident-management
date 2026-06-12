package com.incident.management.dto.response;

import java.util.List;

public record DashboardStatsResponse(
        long totalOpen,
        long totalInProgress,
        long totalResolved,
        long totalClosed,
        List<IncidentResponse> recentIncidents
) {}
