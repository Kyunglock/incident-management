package com.incident.management.service;

import com.incident.management.dto.request.UpdateIncidentDocumentRequest;
import com.incident.management.dto.response.IncidentDocumentResponse;
import com.incident.management.entity.DeploymentPlan;
import com.incident.management.entity.Incident;
import com.incident.management.entity.IncidentAction;
import com.incident.management.entity.IncidentDocument;
import com.incident.management.exception.ResourceNotFoundException;
import com.incident.management.repository.DeploymentPlanRepository;
import com.incident.management.repository.IncidentActionRepository;
import com.incident.management.repository.IncidentDocumentRepository;
import com.incident.management.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentDocumentService {

    private final IncidentDocumentRepository incidentDocumentRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentActionRepository incidentActionRepository;
    private final DeploymentPlanRepository deploymentPlanRepository;
    private final AiService aiService;

    public List<IncidentDocumentResponse> getDocuments(Long incidentId) {
        findIncidentById(incidentId);
        return incidentDocumentRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId)
                .stream().map(IncidentDocumentResponse::from).toList();
    }

    @Transactional
    public IncidentDocumentResponse generateDocument(Long incidentId) {
        Incident incident = findIncidentById(incidentId);
        List<IncidentAction> actions = incidentActionRepository.findByIncidentIdOrderByPerformedAtDesc(incidentId);
        List<DeploymentPlan> plans = deploymentPlanRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId);

        AiService.GeneratedDocument generated = aiService.generateIncidentDocument(incident, actions, plans);

        IncidentDocument document = IncidentDocument.builder()
                .incident(incident)
                .symptom(generated.symptom())
                .rootCause(generated.rootCause())
                .actionTaken(generated.actionTaken())
                .deploymentSummary(generated.deploymentSummary())
                .result(generated.result())
                .isAiGenerated(true)
                .build();

        return IncidentDocumentResponse.from(incidentDocumentRepository.save(document));
    }

    @Transactional
    public IncidentDocumentResponse updateDocument(Long incidentId, Long docId, UpdateIncidentDocumentRequest request) {
        IncidentDocument document = incidentDocumentRepository.findByIdAndIncidentId(docId, incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("문서를 찾을 수 없습니다. ID: " + docId));

        if (request.symptom() != null) document.setSymptom(request.symptom());
        if (request.rootCause() != null) document.setRootCause(request.rootCause());
        if (request.actionTaken() != null) document.setActionTaken(request.actionTaken());
        if (request.deploymentSummary() != null) document.setDeploymentSummary(request.deploymentSummary());
        if (request.result() != null) document.setResult(request.result());
        if (request.reviewedBy() != null) {
            document.setReviewedBy(request.reviewedBy());
            document.setReviewedAt(LocalDateTime.now());
            document.setIsAiGenerated(false);
        }

        return IncidentDocumentResponse.from(incidentDocumentRepository.save(document));
    }

    private Incident findIncidentById(Long incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("장애를 찾을 수 없습니다. ID: " + incidentId));
    }
}
