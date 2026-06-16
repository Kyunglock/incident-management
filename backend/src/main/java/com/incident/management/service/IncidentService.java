package com.incident.management.service;

import com.incident.management.common.DocxRenderer;
import com.incident.management.common.LlmClient;
import com.incident.management.common.PromptBuilder;
import com.incident.management.dto.response.IncidentAnalysisResponse;
import com.incident.management.entity.Document;
import com.incident.management.entity.Incident;
import com.incident.management.entity.ReleaseHistory;
import com.incident.management.repository.DocumentRepository;
import com.incident.management.repository.IncidentRepository;
import com.incident.management.repository.ReleaseHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {

    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final DocxRenderer docxRenderer;
    private final IncidentRepository incidentRepository;
    private final ReleaseHistoryRepository releaseHistoryRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public IncidentAnalysisResponse analyze(
            String symptom,
            String errorLogs,
            Long releaseHistoryId) {
        try {
            String releaseHistorySummary = releaseHistoryId != null
                    ? releaseHistoryRepository.findById(releaseHistoryId)
                        .map(h -> h.getTitle() + "\n" + h.getLlmOutput())
                        .orElse("반영 이력 없음")
                    : "반영 이력 없음";

            String prompt = promptBuilder.buildIncidentAnalysisPrompt(symptom, errorLogs, releaseHistorySummary);
            String llmOutput = llmClient.chat(prompt);
            String docPath = docxRenderer.renderIncidentReport(llmOutput);

            ReleaseHistory releaseHistory = releaseHistoryId != null
                    ? releaseHistoryRepository.findById(releaseHistoryId).orElse(null)
                    : null;

            Incident incident = Incident.builder()
                    .occurredAt(LocalDateTime.now())
                    .symptom(symptom)
                    .cause(llmOutput)
                    .releaseHistory(releaseHistory)
                    .docPath(docPath)
                    .build();
            incident = incidentRepository.save(incident);

            documentRepository.save(Document.builder()
                    .type("INCIDENT_REPORT")
                    .filePath(docPath)
                    .refId(incident.getId())
                    .build());

            return IncidentAnalysisResponse.builder()
                    .id(incident.getId())
                    .symptom(symptom)
                    .cause(llmOutput)
                    .docPath(docPath)
                    .occurredAt(incident.getOccurredAt())
                    .build();
        } catch (Exception e) {
            log.error("장애 분석 실패", e);
            throw new RuntimeException("장애 분석 중 오류: " + e.getMessage());
        }
    }

    public List<IncidentAnalysisResponse> getAll() {
        return incidentRepository.findAllByOrderByOccurredAtDesc().stream()
                .map(i -> IncidentAnalysisResponse.builder()
                        .id(i.getId())
                        .symptom(i.getSymptom())
                        .cause(i.getCause())
                        .docPath(i.getDocPath())
                        .occurredAt(i.getOccurredAt())
                        .build())
                .collect(Collectors.toList());
    }
}
