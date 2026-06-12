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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping
    public ApiResponse<Page<IncidentResponse>> getIncidents(
            @RequestParam(required = false) Incident.IncidentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.ok(incidentService.getIncidents(status, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<IncidentDetailResponse> getIncident(@PathVariable Long id) {
        return ApiResponse.ok(incidentService.getIncidentDetail(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IncidentResponse> createIncident(@Valid @RequestBody CreateIncidentRequest req) {
        return ApiResponse.ok(incidentService.createIncident(req), "장애가 등록되었습니다.");
    }

    @PutMapping("/{id}")
    public ApiResponse<IncidentResponse> updateIncident(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateIncidentRequest req) {
        return ApiResponse.ok(incidentService.updateIncident(id, req));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<IncidentResponse> updateStatus(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateIncidentStatusRequest req) {
        return ApiResponse.ok(incidentService.updateIncidentStatus(id, req));
    }

    @GetMapping("/{id}/actions")
    public ApiResponse<List<IncidentActionResponse>> getActions(@PathVariable Long id) {
        return ApiResponse.ok(incidentService.getActions(id));
    }

    @PostMapping("/{id}/actions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IncidentActionResponse> addAction(@PathVariable Long id,
                                                          @Valid @RequestBody CreateIncidentActionRequest req) {
        return ApiResponse.ok(incidentService.addAction(id, req), "조치가 추가되었습니다.");
    }
}
