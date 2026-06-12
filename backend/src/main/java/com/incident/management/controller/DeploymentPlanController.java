package com.incident.management.controller;

import com.incident.management.dto.request.CreateDeploymentPlanRequest;
import com.incident.management.dto.request.UpdateDeploymentPlanStatusRequest;
import com.incident.management.dto.response.ApiResponse;
import com.incident.management.dto.response.DeploymentPlanResponse;
import com.incident.management.service.DeploymentPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents/{incidentId}/deployment-plans")
@RequiredArgsConstructor
public class DeploymentPlanController {

    private final DeploymentPlanService deploymentPlanService;

    @GetMapping
    public ApiResponse<List<DeploymentPlanResponse>> getPlans(@PathVariable Long incidentId) {
        return ApiResponse.ok(deploymentPlanService.getPlans(incidentId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DeploymentPlanResponse> createPlan(@PathVariable Long incidentId,
                                                           @Valid @RequestBody CreateDeploymentPlanRequest req) {
        return ApiResponse.ok(deploymentPlanService.createPlan(incidentId, req), "반영 계획서가 생성되었습니다.");
    }

    @PatchMapping("/{planId}/status")
    public ApiResponse<DeploymentPlanResponse> updateStatus(@PathVariable Long incidentId,
                                                             @PathVariable Long planId,
                                                             @Valid @RequestBody UpdateDeploymentPlanStatusRequest req) {
        return ApiResponse.ok(deploymentPlanService.updateStatus(incidentId, planId, req));
    }
}
