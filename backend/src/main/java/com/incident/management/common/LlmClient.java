package com.incident.management.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class LlmClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String chatUrl;
    private final String model;
    private final String userAgent;

    public LlmClient(
            WebClient webClient,
            @Value("${ai.llm.url:https://kwaklabs.com/api/v1/kwakai/chat}") String chatUrl,
            @Value("${ai.llm.model:gemma4}") String model,
            @Value("${ai.llm.user-agent:curl/8.4.0}") String userAgent,
            ObjectMapper objectMapper) {
        // 인증서 검증을 완화한 공용 WebClient 빈을 재사용한다.
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.chatUrl = chatUrl;
        this.model = model;
        this.userAgent = userAgent;
    }

    public String chat(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "stream", false
            );

            String response = webClient.post()
                    .uri(chatUrl)
                    // 일부 서버/WAF가 클라이언트 User-Agent로 차단(403)하므로 curl 과 동일하게 보낸다.
                    .header(HttpHeaders.USER_AGENT, userAgent)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            return stripCodeFences(extractContent(root));
        } catch (Exception e) {
            log.error("LLM 호출 실패", e);
            throw new RuntimeException("LLM 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /** 응답 형식 차이에 견디도록 여러 위치에서 본문을 추출한다. */
    private String extractContent(JsonNode root) {
        if (root == null) return "";
        // Ollama 형식: { "message": { "content": "..." } }
        String content = root.path("message").path("content").asText("");
        if (!content.isEmpty()) return content;
        // OpenAI 형식: { "choices": [ { "message": { "content": "..." } } ] }
        content = root.path("choices").path(0).path("message").path("content").asText("");
        if (!content.isEmpty()) return content;
        // 단순 형식: { "response": "..." } 또는 { "content": "..." }
        content = root.path("response").asText("");
        if (!content.isEmpty()) return content;
        return root.path("content").asText("");
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
