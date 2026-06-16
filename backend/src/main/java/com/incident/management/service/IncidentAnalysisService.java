package com.incident.management.service;

import com.incident.management.common.DocxRenderer;
import com.incident.management.common.LlmClient;
import com.incident.management.common.PromptBuilder;
import com.incident.management.dto.response.IncidentAnalysisResponse;
import com.incident.management.entity.Document;
import com.incident.management.entity.Incident;
import com.incident.management.entity.IncidentAnalysis;
import com.incident.management.exception.ResourceNotFoundException;
import com.incident.management.repository.DocumentRepository;
import com.incident.management.repository.IncidentAnalysisRepository;
import com.incident.management.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentAnalysisService {

    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final DocxRenderer docxRenderer;
    private final IncidentRepository incidentRepository;
    private final IncidentAnalysisRepository incidentAnalysisRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public IncidentAnalysisResponse analyze(Long incidentId, String errorLogs) {
        try {
            Incident incident = incidentRepository.findById(incidentId)
                    .orElseThrow(() -> new ResourceNotFoundException("장애 이력을 찾을 수 없습니다: " + incidentId));

            String releaseSummary = "반영이력 #" + incident.getReleaseHistory().getId()
                    + " (반영계획서 #" + incident.getReleaseHistory().getReleasePlan().getId() + ")";

            String prompt = promptBuilder.buildIncidentAnalysisPrompt(incident.getSymptom(), errorLogs, releaseSummary);
            String llmOutput = llmClient.chat(prompt);
            String docPath = docxRenderer.renderIncidentReport(llmOutput);

            IncidentAnalysis analysis = IncidentAnalysis.builder()
                    .incident(incident)
                    .errorLogs(errorLogs)
                    .cause(llmOutput)
                    .docPath(docPath)
                    .build();
            analysis = incidentAnalysisRepository.save(analysis);

            documentRepository.save(Document.builder()
                    .type("INCIDENT_ANALYSIS")
                    .filePath(docPath)
                    .refId(analysis.getId())
                    .build());

            return toResponse(analysis);
        } catch (Exception e) {
            log.error("장애 분석 실패", e);
            throw new RuntimeException("장애 분석 중 오류: " + e.getMessage());
        }
    }

    public List<IncidentAnalysisResponse> getByIncident(Long incidentId) {
        return incidentAnalysisRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private IncidentAnalysisResponse toResponse(IncidentAnalysis analysis) {
        return IncidentAnalysisResponse.builder()
                .id(analysis.getId())
                .incidentId(analysis.getIncident().getId())
                .cause(analysis.getCause())
                .docPath(analysis.getDocPath())
                .createdAt(analysis.getCreatedAt())
                .build();
    }
}
