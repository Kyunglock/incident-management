package com.incident.management.controller;

import com.incident.management.dto.request.CreateIncidentActionRequest;
import com.incident.management.dto.request.CreateIncidentRequest;
import com.incident.management.dto.request.UpdateIncidentRequest;
import com.incident.management.dto.request.UpdateIncidentStatusRequest;
import com.incident.management.dto.response.ApiResponse;
import com.incident.management.dto.response.IncidentActionResponse;
import com.incident.management.dto.response.IncidentDetailResponse;
import com.incident.management.dto.response.IncidentResponse;
import com.incident.management.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(incidentService.getIncidents(page, size, status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IncidentDetailResponse>> getIncident(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(incidentService.getIncidentDetail(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IncidentResponse>> createIncident(
            @Valid @RequestBody CreateIncidentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(incidentService.createIncident(request), "장애가 등록되었습니다."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<IncidentResponse>> updateIncident(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIncidentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(incidentService.updateIncident(id, request), "장애가 수정되었습니다."));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<IncidentResponse>> updateIncidentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIncidentStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                incidentService.updateIncidentStatus(id, request.status()), "상태가 변경되었습니다."));
    }

    @GetMapping("/{id}/actions")
    public ResponseEntity<ApiResponse<List<IncidentActionResponse>>> getIncidentActions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(incidentService.getIncidentActions(id)));
    }

    @PostMapping("/{id}/actions")
    public ResponseEntity<ApiResponse<IncidentActionResponse>> addIncidentAction(
            @PathVariable Long id,
            @Valid @RequestBody CreateIncidentActionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(incidentService.addIncidentAction(id, request), "조치 내역이 추가되었습니다."));
    }
}
