package com.incident.management.controller;

import com.incident.management.dto.request.CreateDeploymentPlanRequest;
import com.incident.management.dto.request.UpdateDeploymentPlanRequest;
import com.incident.management.dto.request.UpdateDeploymentPlanStatusRequest;
import com.incident.management.dto.response.ApiResponse;
import com.incident.management.dto.response.DeploymentPlanResponse;
import com.incident.management.service.DeploymentPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents/{incidentId}/deployment-plans")
@RequiredArgsConstructor
public class DeploymentPlanController {

    private final DeploymentPlanService deploymentPlanService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeploymentPlanResponse>>> getDeploymentPlans(@PathVariable Long incidentId) {
        return ResponseEntity.ok(ApiResponse.success(deploymentPlanService.getDeploymentPlans(incidentId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeploymentPlanResponse>> createDeploymentPlan(
            @PathVariable Long incidentId,
            @Valid @RequestBody CreateDeploymentPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(deploymentPlanService.createDeploymentPlan(incidentId, request), "반영 계획서가 생성되었습니다."));
    }

    @PutMapping("/{planId}")
    public ResponseEntity<ApiResponse<DeploymentPlanResponse>> updateDeploymentPlan(
            @PathVariable Long incidentId,
            @PathVariable Long planId,
            @RequestBody UpdateDeploymentPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                deploymentPlanService.updateDeploymentPlan(incidentId, planId, request), "반영 계획서가 수정되었습니다."));
    }

    @PatchMapping("/{planId}/status")
    public ResponseEntity<ApiResponse<DeploymentPlanResponse>> updateDeploymentPlanStatus(
            @PathVariable Long incidentId,
            @PathVariable Long planId,
            @Valid @RequestBody UpdateDeploymentPlanStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                deploymentPlanService.updateDeploymentPlanStatus(incidentId, planId, request.status(), request.approvedBy()),
                "상태가 변경되었습니다."));
    }
}
