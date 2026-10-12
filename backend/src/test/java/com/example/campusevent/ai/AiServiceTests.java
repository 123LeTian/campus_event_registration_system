package com.example.campusevent.ai;

import com.example.campusevent.ai.client.AiModelClient;
import com.example.campusevent.ai.dto.EventIntroductionRequest;
import com.example.campusevent.ai.service.impl.AiServiceImpl;
import com.example.campusevent.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiServiceTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void invalidAiResponseReturnsBusinessError() {
        AiModelClient modelClient = mock(AiModelClient.class);
        when(modelClient.generate(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("不是 JSON");
        AiServiceImpl service = new AiServiceImpl(objectMapper, modelClient);

        assertThatThrownBy(() -> service.generateEventIntroduction(validRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 返回内容异常，请重新生成或手工填写");
    }

    private EventIntroductionRequest validRequest() {
        EventIntroductionRequest request = new EventIntroductionRequest();
        request.setTitle("2026校园科技文化节");
        request.setLocation("大学生活动中心");
        request.setStartTime(LocalDateTime.now().plusDays(5));
        request.setEndTime(LocalDateTime.now().plusDays(5).plusHours(2));
        request.setAudience("全校学生");
        request.setActivityType("校园科技");
        request.setKeywords("创新、社团、实践");
        return request;
    }
}
