package com.incident.management.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.management.entity.DeploymentPlan;
import com.incident.management.entity.Incident;
import com.incident.management.entity.IncidentAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.llm.url}")
    private String llmUrl;

    public record GeneratedDocument(
            String symptom,
            String rootCause,
            String actionTaken,
            String deploymentSummary,
            String result
    ) {}

    public GeneratedDocument generateIncidentDocument(Incident incident, List<IncidentAction> actions, List<DeploymentPlan> deploymentPlans) {
        String prompt = buildPrompt(incident, actions, deploymentPlans);

        try {
            Map<String, Object> requestBody = Map.of(
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            String responseBody = webClient.post()
                    .uri(llmUrl)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseResponse(responseBody);
        } catch (Exception e) {
            log.error("LLM API 호출 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("AI 문서 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private String buildPrompt(Incident incident, List<IncidentAction> actions, List<DeploymentPlan> deploymentPlans) {
        StringBuilder actionsText = new StringBuilder();
        for (IncidentAction action : actions) {
            actionsText.append(String.format("- [%s] %s (담당: %s, 시각: %s)\n",
                    action.getActionType().name(),
                    action.getActionDescription(),
                    action.getPerformedBy() != null ? action.getPerformedBy() : "미지정",
                    action.getPerformedAt() != null ? action.getPerformedAt().toString() : "미지정"));
        }

        StringBuilder deploymentText = new StringBuilder();
        for (DeploymentPlan plan : deploymentPlans) {
            deploymentText.append(String.format("- [%s] %s (제출자: %s)\n",
                    plan.getStatus().name(),
                    plan.getPlanContent(),
                    plan.getSubmittedBy() != null ? plan.getSubmittedBy() : "미지정"));
        }

        return String.format("""
                다음 장애 처리 정보를 바탕으로 장애 처리 문서를 작성해주세요.
                
                [장애 정보]
                제목: %s
                증상: %s
                담당자: %s
                
                [조치 내역]
                %s
                
                [반영 계획]
                %s
                
                다음 JSON 형식으로만 응답해주세요:
                {
                  "symptom": "증상 요약",
                  "root_cause": "근본 원인 분석",
                  "action_taken": "조치 내용 요약",
                  "deployment_summary": "반영 내역 요약",
                  "result": "처리 결과 및 재발 방지 방안"
                }
                """,
                incident.getTitle(),
                incident.getDescription() != null ? incident.getDescription() : "설명 없음",
                incident.getAssigneeName() != null ? incident.getAssigneeName() : "미지정",
                actionsText.length() > 0 ? actionsText.toString() : "조치 내역 없음",
                deploymentText.length() > 0 ? deploymentText.toString() : "반영 계획 없음"
        );
    }

    private GeneratedDocument parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            String jsonContent = extractJson(content);
            JsonNode docNode = objectMapper.readTree(jsonContent);
            return new GeneratedDocument(
                    docNode.path("symptom").asText(""),
                    docNode.path("root_cause").asText(""),
                    docNode.path("action_taken").asText(""),
                    docNode.path("deployment_summary").asText(""),
                    docNode.path("result").asText("")
            );
        } catch (Exception e) {
            log.warn("LLM 응답 JSON 파싱 실패, 원본 텍스트 사용: {}", e.getMessage());
            try {
                JsonNode root = objectMapper.readTree(responseBody);
                String content = root.path("choices").get(0).path("message").path("content").asText();
                return new GeneratedDocument(content, "", "", "", "");
            } catch (Exception ex) {
                return new GeneratedDocument(responseBody, "", "", "", "");
            }
        }
    }

    private String extractJson(String content) {
        if (content.contains("```json")) {
            int start = content.indexOf("```json") + 7;
            int end = content.lastIndexOf("```");
            if (end > start) {
                return content.substring(start, end).trim();
            }
        }
        if (content.contains("```")) {
            int start = content.indexOf("```") + 3;
            int end = content.lastIndexOf("```");
            if (end > start) {
                return content.substring(start, end).trim();
            }
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }
}
