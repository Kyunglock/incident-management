package com.incident.management.service;

import com.incident.management.dto.request.CreateDeploymentPlanRequest;
import com.incident.management.dto.request.UpdateDeploymentPlanRequest;
import com.incident.management.dto.response.DeploymentPlanResponse;
import com.incident.management.entity.DeploymentPlan;
import com.incident.management.entity.Incident;
import com.incident.management.exception.ResourceNotFoundException;
import com.incident.management.repository.DeploymentPlanRepository;
import com.incident.management.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeploymentPlanService {

    private final DeploymentPlanRepository deploymentPlanRepository;
    private final IncidentRepository incidentRepository;

    public List<DeploymentPlanResponse> getDeploymentPlans(Long incidentId) {
        findIncidentById(incidentId);
        return deploymentPlanRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId)
                .stream().map(DeploymentPlanResponse::from).toList();
    }

    @Transactional
    public DeploymentPlanResponse createDeploymentPlan(Long incidentId, CreateDeploymentPlanRequest request) {
        Incident incident = findIncidentById(incidentId);

        DeploymentPlan plan = DeploymentPlan.builder()
                .incident(incident)
                .planContent(request.planContent())
                .submittedBy(request.submittedBy())
                .build();

        return DeploymentPlanResponse.from(deploymentPlanRepository.save(plan));
    }

    @Transactional
    public DeploymentPlanResponse updateDeploymentPlan(Long incidentId, Long planId, UpdateDeploymentPlanRequest request) {
        DeploymentPlan plan = findPlanByIdAndIncidentId(planId, incidentId);

        if (request.planContent() != null) plan.setPlanContent(request.planContent());
        if (request.submittedBy() != null) plan.setSubmittedBy(request.submittedBy());

        return DeploymentPlanResponse.from(deploymentPlanRepository.save(plan));
    }

    @Transactional
    public DeploymentPlanResponse updateDeploymentPlanStatus(Long incidentId, Long planId, String status, String approvedBy) {
        DeploymentPlan plan = findPlanByIdAndIncidentId(planId, incidentId);
        DeploymentPlan.PlanStatus newStatus = DeploymentPlan.PlanStatus.valueOf(status.toUpperCase());
        plan.setStatus(newStatus);

        if (newStatus == DeploymentPlan.PlanStatus.SUBMITTED) {
            plan.setSubmittedAt(LocalDateTime.now());
        } else if (newStatus == DeploymentPlan.PlanStatus.APPROVED) {
            plan.setApprovedAt(LocalDateTime.now());
            if (approvedBy != null) plan.setApprovedBy(approvedBy);
        }

        return DeploymentPlanResponse.from(deploymentPlanRepository.save(plan));
    }

    private Incident findIncidentById(Long incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("장애를 찾을 수 없습니다. ID: " + incidentId));
    }

    private DeploymentPlan findPlanByIdAndIncidentId(Long planId, Long incidentId) {
        return deploymentPlanRepository.findByIdAndIncidentId(planId, incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("반영 계획서를 찾을 수 없습니다. ID: " + planId));
    }
}
