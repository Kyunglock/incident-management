package com.incident.management.controller;

import com.incident.management.dto.response.IncidentAnalysisResponse;
import com.incident.management.service.IncidentAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents/{incidentId}/analyses")
@RequiredArgsConstructor
public class IncidentAnalysisController {

    private final IncidentAnalysisService incidentAnalysisService;

    @PostMapping
    public ResponseEntity<IncidentAnalysisResponse> analyze(
            @PathVariable Long incidentId,
            @RequestParam(required = false) String errorLogs) {
        return ResponseEntity.ok(incidentAnalysisService.analyze(incidentId, errorLogs));
    }

    @GetMapping
    public ResponseEntity<List<IncidentAnalysisResponse>> getByIncident(@PathVariable Long incidentId) {
        return ResponseEntity.ok(incidentAnalysisService.getByIncident(incidentId));
    }
}
