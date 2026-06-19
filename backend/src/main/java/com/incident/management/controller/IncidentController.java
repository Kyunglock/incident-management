package com.incident.management.controller;

import com.incident.management.dto.response.IncidentResponse;
import com.incident.management.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping("/api/release-histories/{historyId}/incidents")
    public ResponseEntity<IncidentResponse> create(
            @PathVariable Long historyId,
            @RequestParam String symptom,
            @RequestParam(required = false) LocalDateTime occurredAt) {
        return ResponseEntity.ok(incidentService.create(historyId, symptom, occurredAt));
    }

    @GetMapping("/api/release-histories/{historyId}/incidents")
    public ResponseEntity<List<IncidentResponse>> getByHistory(@PathVariable Long historyId) {
        return ResponseEntity.ok(incidentService.getByHistory(historyId));
    }

    @GetMapping("/api/incidents/{id}")
    public ResponseEntity<IncidentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.getById(id));
    }

    /** 전역 장애 목록 (모든 SR) */
    @GetMapping("/api/incidents")
    public ResponseEntity<List<IncidentResponse>> getAll() {
        return ResponseEntity.ok(incidentService.getAll());
    }
}
