package com.incident.management.controller;

import com.incident.management.dto.request.CreateDeploymentPlanRequest;
import com.incident.management.dto.request.UpdateDeploymentPlanRequest;
import com.incident.management.dto.request.UpdateDeploymentPlanStatusRequest;
import com.incident.management.dto.response.ApiResponse;
import com.incident.management.dto.response.DeploymentPlanResponse;
import com.incident.management.service.DeploymentPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents/{incidentId}/deployment-plans")
@RequiredArgsConstructor
public class DeploymentPlanController {

    private final DeploymentPlanService deploymentPlanService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeploymentPlanResponse>>> getPlans(@PathVariable Long incidentId) {
        return ResponseEntity.ok(ApiResponse.ok(deploymentPlanService.getPlans(incidentId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeploymentPlanResponse>> createPlan(
            @PathVariable Long incidentId,
            @Valid @RequestBody CreateDeploymentPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(deploymentPlanService.createPlan(incidentId, request), "Deployment plan created successfully"));
    }

    @PutMapping("/{planId}")
    public ResponseEntity<ApiResponse<DeploymentPlanResponse>> updatePlan(
            @PathVariable Long incidentId,
            @PathVariable Long planId,
            @RequestBody UpdateDeploymentPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(deploymentPlanService.updatePlan(incidentId, planId, request)));
    }

    @PatchMapping("/{planId}/status")
    public ResponseEntity<ApiResponse<DeploymentPlanResponse>> updatePlanStatus(
            @PathVariable Long incidentId,
            @PathVariable Long planId,
            @Valid @RequestBody UpdateDeploymentPlanStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(deploymentPlanService.updateStatus(incidentId, planId, request)));
    }
}
