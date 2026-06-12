package com.incident.management.service;

import com.incident.management.dto.request.UpdateIncidentDocumentRequest;
import com.incident.management.dto.response.IncidentDocumentResponse;
import com.incident.management.entity.IncidentDocument;
import com.incident.management.repository.IncidentDocumentRepository;
import com.incident.management.repository.IncidentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class IncidentDocumentService {

    private final IncidentDocumentRepository incidentDocumentRepository;
    private final IncidentRepository incidentRepository;

    @Transactional(readOnly = true)
    public List<IncidentDocumentResponse> getDocuments(Long incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new EntityNotFoundException("Incident not found with id: " + incidentId);
        }
        return incidentDocumentRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId)
                .stream()
                .map(IncidentDocumentResponse::from)
                .toList();
    }

    public IncidentDocumentResponse updateDocument(Long incidentId, Long docId, UpdateIncidentDocumentRequest request) {
        IncidentDocument doc = incidentDocumentRepository.findById(docId)
                .orElseThrow(() -> new EntityNotFoundException("IncidentDocument not found with id: " + docId));

        if (!doc.getIncident().getId().equals(incidentId)) {
            throw new EntityNotFoundException("Document not found for incident: " + incidentId);
        }

        if (request.symptom() != null) doc.setSymptom(request.symptom());
        if (request.rootCause() != null) doc.setRootCause(request.rootCause());
        if (request.actionTaken() != null) doc.setActionTaken(request.actionTaken());
        if (request.deploymentSummary() != null) doc.setDeploymentSummary(request.deploymentSummary());
        if (request.result() != null) doc.setResult(request.result());

        if (request.reviewedBy() != null) {
            doc.setReviewedBy(request.reviewedBy());
            doc.setReviewedAt(LocalDateTime.now());
            doc.setIsAiGenerated(false);
        }

        return IncidentDocumentResponse.from(incidentDocumentRepository.save(doc));
    }
}
