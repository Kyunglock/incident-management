package com.incident.management.controller;

import com.incident.management.dto.response.IncidentAnalysisResponse;
import com.incident.management.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incident")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping("/analyze")
    public ResponseEntity<IncidentAnalysisResponse> analyze(
            @RequestParam String symptom,
            @RequestParam(required = false) String errorLogs,
            @RequestParam(required = false) Long releaseHistoryId) {
        return ResponseEntity.ok(incidentService.analyze(symptom, errorLogs, releaseHistoryId));
    }

    @GetMapping
    public ResponseEntity<List<IncidentAnalysisResponse>> getAll() {
        return ResponseEntity.ok(incidentService.getAll());
    }
}
