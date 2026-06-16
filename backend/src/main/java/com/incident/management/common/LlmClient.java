package com.incident.management.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class LlmClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public LlmClient(
            @Value("${ai.llm.base-url}") String baseUrl,
            @Value("${ai.llm.model}") String model,
            ObjectMapper objectMapper) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.model = model;
    }

    public String chat(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "stream", false
            );

            String response = webClient.post()
                    .uri("/api/chat")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            String content = root.path("message").path("content").asText();
            return stripCodeFences(content);
        } catch (Exception e) {
            log.error("LLM 호출 실패", e);
            throw new RuntimeException("LLM 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private String stripCodeFences(String content) {
        if (content == null) return "";
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("```[a-zA-Z]*\\n?", "");
            int endIdx = trimmed.lastIndexOf("```");
            if (endIdx >= 0) {
                trimmed = trimmed.substring(0, endIdx);
            }
        }
        return trimmed.trim();
    }
}
