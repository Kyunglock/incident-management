package com.incident.management.controller;

import com.incident.management.dto.request.CreateIncidentActionRequest;
import com.incident.management.dto.request.CreateIncidentRequest;
import com.incident.management.dto.request.UpdateIncidentRequest;
import com.incident.management.dto.request.UpdateIncidentStatusRequest;
import com.incident.management.dto.response.ApiResponse;
import com.incident.management.dto.response.IncidentActionResponse;
import com.incident.management.dto.response.IncidentDetailResponse;
import com.incident.management.dto.response.IncidentResponse;
import com.incident.management.entity.Incident;
import com.incident.management.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<IncidentResponse>>> getIncidents(
            @RequestParam(required = false) Incident.IncidentStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(incidentService.getIncidents(status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IncidentDetailResponse>> getIncident(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(incidentService.getIncidentDetail(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IncidentResponse>> createIncident(
            @Valid @RequestBody CreateIncidentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(incidentService.createIncident(request), "Incident created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<IncidentResponse>> updateIncident(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIncidentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(incidentService.updateIncident(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<IncidentResponse>> updateIncidentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIncidentStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(incidentService.updateIncidentStatus(id, request)));
    }

    @GetMapping("/{id}/actions")
    public ResponseEntity<ApiResponse<List<IncidentActionResponse>>> getActions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(incidentService.getActions(id)));
    }

    @PostMapping("/{id}/actions")
    public ResponseEntity<ApiResponse<IncidentActionResponse>> addAction(
            @PathVariable Long id,
            @Valid @RequestBody CreateIncidentActionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(incidentService.addAction(id, request), "Action added successfully"));
    }
}
