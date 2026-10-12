package com.example.campusevent.ai.client;

import com.example.campusevent.common.api.ErrorCode;
import com.example.campusevent.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * Small OpenAI-compatible client. The client returns model text only; parsing and
 * business validation stay in AiServiceImpl.
 */
@Component
public class AiModelClient {

    private static final Logger log = LoggerFactory.getLogger(AiModelClient.class);

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final boolean mockEnabled;
    private final long timeoutMs;

    public AiModelClient(
            ObjectMapper objectMapper,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.model:gpt-4o-mini}") String model,
            @Value("${app.ai.base-url:https://api.openai.com/v1/chat/completions}") String baseUrl,
            @Value("${app.ai.mock-enabled:false}") boolean mockEnabled,
            @Value("${app.ai.timeout-ms:15000}") long timeoutMs
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.mockEnabled = mockEnabled;
        this.timeoutMs = timeoutMs;
    }

    public String generate(String systemPrompt, String userPrompt) {
        if (mockEnabled) {
            return mockResponse(userPrompt);
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AI generation skipped because AI_API_KEY is not configured");
            throw new BusinessException(
                    ErrorCode.AI_NOT_CONFIGURED,
                    "AI 服务未配置，请手工填写",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.7);
            ArrayNode messages = requestBody.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", userPrompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(Duration.ofMillis(effectiveTimeoutMs()))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(effectiveTimeoutMs()))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("AI service returned HTTP status {}", response.statusCode());
                throw unavailable();
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                log.warn("AI service returned an empty content payload");
                throw invalidResponse();
            }
            return content;
        } catch (HttpTimeoutException ex) {
            throw timeout(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw timeout(ex);
        } catch (JsonProcessingException ex) {
            log.warn("AI request or response JSON processing failed");
            throw unavailable();
        } catch (IOException | IllegalArgumentException ex) {
            log.warn("AI service request failed: {}", ex.getClass().getSimpleName());
            throw unavailable();
        }
    }

    private long effectiveTimeoutMs() {
        return Math.max(timeoutMs, 1000L);
    }

    private String mockResponse(String userPrompt) {
        String title = "校园活动";
        String location = "校内场地";
        String audience = "对校园活动感兴趣的师生";
        try {
            int marker = userPrompt.lastIndexOf("活动上下文(JSON)：");
            if (marker >= 0) {
                JsonNode context = objectMapper.readTree(userPrompt.substring(marker + "活动上下文(JSON)：".length()).trim());
                title = context.path("title").asText(title);
                location = context.path("location").asText(location);
                audience = context.path("audience").asText(audience);
                if (audience.isBlank()) {
                    audience = "对校园活动感兴趣的师生";
                }
            }
            ObjectNode result = objectMapper.createObjectNode();
            if (userPrompt.contains("活动文案与活动介绍")) {
                result.put("promotion", "在“" + title + "”中走进" + location + "，和同伴一起完成一次有收获的校园体验。");
                result.put("introduction", "“" + title + "”将在" + location
                        + "开展，活动面向" + audience + "，通过现场参与、交流互动和实践体验，帮助同学拓展视野、认识伙伴并获得真实的校园活动体验。");
            } else if (userPrompt.contains("宣传文案")) {
                result.put("promotion", "欢迎参加“" + title + "”！活动将在" + location
                        + "举行，欢迎" + audience + "到场交流、体验与收获。");
            } else {
                result.put("introduction", "“" + title + "”将在" + location
                        + "开展，活动面向" + audience + "，通过现场参与、交流互动和实践体验，帮助同学拓展视野、认识伙伴并获得真实的校园活动体验。");
            }
            ArrayNode highlights = result.putArray("highlights");
            highlights.add("校园场景真实，参与方式清晰");
            highlights.add("强调交流互动，适合共同参与");
            highlights.add("内容积极健康，便于组织和传播");
            result.put("audience", audience);
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            throw unavailable();
        }
    }

    private BusinessException timeout(Exception cause) {
        log.warn("AI service timed out: {}", cause.getClass().getSimpleName());
        return new BusinessException(
                ErrorCode.AI_TIMEOUT,
                "AI 生成超时，请稍后重试或手工填写",
                HttpStatus.GATEWAY_TIMEOUT
        );
    }

    private BusinessException unavailable() {
        return new BusinessException(
                ErrorCode.AI_SERVICE_UNAVAILABLE,
                "AI 生成暂不可用，请手工填写",
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    private BusinessException invalidResponse() {
        return new BusinessException(
                ErrorCode.AI_RESPONSE_INVALID,
                "AI 返回内容异常，请重新生成或手工填写",
                HttpStatus.BAD_GATEWAY
        );
    }
}
