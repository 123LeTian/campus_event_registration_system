package com.example.campusevent.ai.service.impl;

import com.example.campusevent.ai.client.AiModelClient;
import com.example.campusevent.ai.dto.EventIntroductionRequest;
import com.example.campusevent.ai.dto.EventPromotionRequest;
import com.example.campusevent.ai.service.AiService;
import com.example.campusevent.ai.vo.AiGenerateVO;
import com.example.campusevent.common.api.ErrorCode;
import com.example.campusevent.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiServiceImpl implements AiService {

    private static final String SYSTEM_PROMPT = """
            你是校园活动内容编辑。只生成适合高校活动场景的自然、积极、克制的中文内容。
            必须只返回有效 JSON，不要 Markdown 代码块，不要解释文字。
            不要虚构奖项、证书、学分、官方背书、嘉宾身份或其他未提供的承诺。
            """;

    private final ObjectMapper objectMapper;
    private final AiModelClient modelClient;

    public AiServiceImpl(ObjectMapper objectMapper, AiModelClient modelClient) {
        this.objectMapper = objectMapper;
        this.modelClient = modelClient;
    }

    @Override
    public AiGenerateVO generateEventIntroduction(EventIntroductionRequest request) {
        String prompt = buildPrompt(
                "活动文案与活动介绍",
                request.getTitle(),
                request.getLocation(),
                request.getStartTime(),
                request.getEndTime(),
                request.getAudience(),
                request.getActivityType(),
                request.getKeywords(),
                null
        );
        return parseIntroduction(modelClient.generate(SYSTEM_PROMPT, prompt));
    }

    @Override
    public AiGenerateVO generateEventPromotion(EventPromotionRequest request) {
        validateTimes(request.getStartTime(), request.getEndTime());
        String prompt = buildPrompt(
                "宣传文案",
                request.getTitle(),
                request.getLocation(),
                request.getStartTime(),
                request.getEndTime(),
                request.getAudience(),
                request.getActivityType(),
                request.getKeywords(),
                request.getDescription()
        );
        return parsePromotion(modelClient.generate(SYSTEM_PROMPT, prompt));
    }

    private String buildPrompt(
            String task,
            String title,
            String location,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String audience,
            String activityType,
            String keywords,
            String description
    ) {
        ObjectNode context = objectMapper.createObjectNode();
        context.put("title", trim(title));
        context.put("location", trim(location));
        context.put("startTime", startTime == null ? "" : startTime.toString());
        context.put("endTime", endTime == null ? "" : endTime.toString());
        context.put("audience", trim(audience));
        context.put("activityType", trim(activityType));
        context.put("keywords", trim(keywords));
        if (description != null) {
            context.put("description", trim(description));
        }
        String format = "活动文案与活动介绍".equals(task)
                ? "{\"promotion\":\"一句适合卡片展示和传播的短文案\",\"introduction\":\"100到300字正文\",\"highlights\":[\"亮点1\",\"亮点2\",\"亮点3\"],\"audience\":\"适合人群\"}"
                : "{\"promotion\":\"适合通知、海报或社群转发的短文案\",\"highlights\":[\"亮点1\",\"亮点2\",\"亮点3\"],\"audience\":\"适合人群\"}";
        return "任务类型：" + task + "\n请根据下面的活动上下文生成 JSON，至少提供 3 个亮点。\n"
                + "返回格式：" + format + "\n活动上下文(JSON)：\n" + context;
    }

    private AiGenerateVO parseIntroduction(String raw) {
        JsonNode root = parseRoot(raw);
        String promotion = requiredText(root, "promotion");
        String introduction = requiredText(root, "introduction");
        return AiGenerateVO.copyAndIntroduction(promotion, introduction, requiredHighlights(root), requiredText(root, "audience"));
    }

    private AiGenerateVO parsePromotion(String raw) {
        JsonNode root = parseRoot(raw);
        String promotion = requiredText(root, "promotion");
        return AiGenerateVO.promotion(promotion, requiredHighlights(root), requiredText(root, "audience"));
    }

    private JsonNode parseRoot(String raw) {
        if (raw == null || raw.isBlank()) {
            throw invalidResponse();
        }
        String content = raw.trim();
        if (content.startsWith("```")) {
            int firstNewline = content.indexOf('\n');
            int lastFence = content.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                content = content.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int firstBrace = content.indexOf('{');
        int lastBrace = content.lastIndexOf('}');
        if (firstBrace < 0 || lastBrace <= firstBrace) {
            throw invalidResponse();
        }
        try {
            JsonNode root = objectMapper.readTree(content.substring(firstBrace, lastBrace + 1));
            if (root.has("data") && root.path("data").isObject()) {
                return root.path("data");
            }
            if (!root.isObject()) {
                throw invalidResponse();
            }
            return root;
        } catch (JsonProcessingException ex) {
            throw invalidResponse();
        }
    }

    private String requiredText(JsonNode root, String field) {
        String value = root.path(field).asText("").trim();
        if (value.isBlank()) {
            throw invalidResponse();
        }
        return value;
    }

    private List<String> requiredHighlights(JsonNode root) {
        JsonNode node = root.path("highlights");
        if (!node.isArray()) {
            throw invalidResponse();
        }
        List<String> highlights = new ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText("").trim();
            if (!value.isBlank()) {
                highlights.add(value);
            }
        }
        if (highlights.size() < 3) {
            throw invalidResponse();
        }
        return highlights;
    }

    private void validateTimes(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "结束时间必须晚于开始时间",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private BusinessException invalidResponse() {
        return new BusinessException(
                ErrorCode.AI_RESPONSE_INVALID,
                "AI 返回内容异常，请重新生成或手工填写",
                HttpStatus.BAD_GATEWAY
        );
    }
}
