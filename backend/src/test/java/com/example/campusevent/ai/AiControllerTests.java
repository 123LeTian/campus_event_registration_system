package com.example.campusevent.ai;

import com.example.campusevent.common.security.JwtTokenProvider;
import com.example.campusevent.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AiControllerTests {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void organizerGeneratesIntroductionSuccessfully() throws Exception {
        mockMvc.perform(post("/api/ai/event-introduction")
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aiPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.promotion").isNotEmpty())
                .andExpect(jsonPath("$.data.introduction").isNotEmpty())
                .andExpect(jsonPath("$.data.highlights.length()").value(3))
                .andExpect(jsonPath("$.data.audience").value("全校学生"));
    }

    @Test
    void adminGeneratesIntroductionSuccessfully() throws Exception {
        mockMvc.perform(post("/api/ai/event-introduction")
                        .header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aiPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.promotion").isNotEmpty())
                .andExpect(jsonPath("$.data.introduction").isNotEmpty())
                .andExpect(jsonPath("$.data.highlights.length()").value(3))
                .andExpect(jsonPath("$.data.audience").value("全校学生"));
    }

    @Test
    void organizerGeneratesIntroductionWithoutTimes() throws Exception {
        mockMvc.perform(post("/api/ai/event-introduction")
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "2026校园科技文化节",
                                  "location": "大学生活动中心",
                                  "audience": "全校学生",
                                  "activityType": "校园科技",
                                  "keywords": "创新、社团、实践"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.promotion").isNotEmpty())
                .andExpect(jsonPath("$.data.introduction").isNotEmpty())
                .andExpect(jsonPath("$.data.highlights.length()").value(3));
    }

    @Test
    void introductionDoesNotValidateTimeOrder() throws Exception {
        ObjectNode payload = (ObjectNode) objectMapper.readTree(aiPayload());
        payload.put("endTime", "2000-01-01T00:00:00");

        mockMvc.perform(post("/api/ai/event-introduction")
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.promotion").isNotEmpty())
                .andExpect(jsonPath("$.data.introduction").isNotEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "title, 标题不能为空",
            "location, 地点不能为空",
            "activityType, 请填写活动类型",
            "audience, 请填写适合人群",
            "keywords, 请填写关键词"
    })
    void missingWritingContextReturnsSpecificMessage(String field, String message) throws Exception {
        for (String value : new String[]{null, "", "   "}) {
            ObjectNode payload = (ObjectNode) objectMapper.readTree(aiPayload());
            payload.remove("startTime");
            payload.remove("endTime");
            if (value == null) {
                payload.remove(field);
            } else {
                payload.put(field, value);
            }

            mockMvc.perform(post("/api/ai/event-introduction")
                            .header("Authorization", bearer("organizer1"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000))
                    .andExpect(jsonPath("$.message", containsString(message)))
                    .andExpect(jsonPath("$.message", not(containsString("startTime"))))
                    .andExpect(jsonPath("$.message", not(containsString("endTime"))));
        }
    }

    @Test
    void manualEventSaveDoesNotRequireAiContext() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventPayload("手动填写的活动文案", "手动填写的活动介绍")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.copywriting").value("手动填写的活动文案"))
                .andExpect(jsonPath("$.data.description").value("手动填写的活动介绍"));
    }

    @Test
    void savingEventStillRequiresTimes() throws Exception {
        ObjectNode payload = (ObjectNode) objectMapper.readTree(
                eventPayload("活动文案", "活动介绍"));
        payload.remove("startTime");
        payload.remove("endTime");
        payload.remove("registrationDeadline");

        mockMvc.perform(post("/api/events")
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("开始时间不能为空")))
                .andExpect(jsonPath("$.message", containsString("结束时间不能为空")))
                .andExpect(jsonPath("$.message", containsString("报名截止时间不能为空")));
    }

    @Test
    void studentCannotGenerateIntroduction() throws Exception {
        mockMvc.perform(post("/api/ai/event-introduction")
                        .header("Authorization", bearer("studentA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aiPayload()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void anonymousCannotGenerateIntroduction() throws Exception {
        mockMvc.perform(post("/api/ai/event-introduction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aiPayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void missingTitleOrLocationReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/ai/event-introduction")
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aiPayload("   ", "大学生活动中心")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        mockMvc.perform(post("/api/ai/event-introduction")
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aiPayload("2026校园科技文化节", "   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void mockModePromotionReturnsPromotion() throws Exception {
        mockMvc.perform(post("/api/ai/event-promotion")
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "2026校园科技文化节",
                                  "location": "大学生活动中心",
                                  "startTime": "%s",
                                  "endTime": "%s",
                                  "audience": "全校学生",
                                  "activityType": "校园科技",
                                  "keywords": "创新、社团、实践",
                                  "description": "人工确认后的活动介绍"
                                }
                                """.formatted(startTime(), endTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.promotion").isNotEmpty())
                .andExpect(jsonPath("$.data.highlights.length()").value(3))
                .andExpect(jsonPath("$.data.audience").value("全校学生"));
    }

    @Test
    void aiEndpointsDoNotWriteEventTable() throws Exception {
        Integer before = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM event", Integer.class);

        mockMvc.perform(post("/api/ai/event-introduction")
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aiPayload()))
                .andExpect(status().isOk());

        Integer after = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM event", Integer.class);
        assertThat(after).isEqualTo(before);
    }

    @Test
    void confirmedAiIntroductionCanBeSavedAsEventDescription() throws Exception {
        String aiResponse = mockMvc.perform(post("/api/ai/event-introduction")
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aiPayload()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String confirmedDescription = objectMapper.readTree(aiResponse).at("/data/introduction").asText()
                + " 这是组织者人工修改后的补充。";
        String confirmedCopywriting = objectMapper.readTree(aiResponse).at("/data/promotion").asText()
                + " 人工确认。";

        String createResponse = mockMvc.perform(post("/api/events")
                        .header("Authorization", bearer("organizer1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventPayload(confirmedCopywriting, confirmedDescription)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.copywriting").value(confirmedCopywriting))
                .andExpect(jsonPath("$.data.description").value(confirmedDescription))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createResponse);
        Long id = created.at("/data/id").asLong();
        mockMvc.perform(get("/api/events/{id}", id)
                        .header("Authorization", bearer("organizer1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.copywriting").value(confirmedCopywriting))
                .andExpect(jsonPath("$.data.description").value(confirmedDescription));
    }

    private String bearer(String username) {
        return "Bearer " + jwtTokenProvider.createToken(userMapper.findByUsername(username));
    }

    private String aiPayload() {
        return aiPayload("2026校园科技文化节", "大学生活动中心");
    }

    private String aiPayload(String title, String location) {
        return """
                {
                  "title": "%s",
                  "location": "%s",
                  "startTime": "%s",
                  "endTime": "%s",
                  "audience": "全校学生",
                  "activityType": "校园科技",
                  "keywords": "创新、社团、实践"
                }
                """.formatted(title, location, startTime(), endTime());
    }

    private String eventPayload(String copywriting, String description) {
        LocalDateTime start = LocalDateTime.now().plusDays(5).withNano(0);
        return """
                {
                  "title": "2026校园科技文化节",
                  "copywriting": %s,
                  "description": %s,
                  "location": "大学生活动中心",
                  "startTime": "%s",
                  "endTime": "%s",
                  "registrationDeadline": "%s",
                  "maxParticipants": 80
                }
                """.formatted(
                toJson(copywriting),
                toJson(description),
                start.format(FORMATTER),
                start.plusHours(2).format(FORMATTER),
                start.minusDays(1).format(FORMATTER)
        );
    }

    private String toJson(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String startTime() {
        return LocalDateTime.now().plusDays(5).withNano(0).format(FORMATTER);
    }

    private String endTime() {
        return LocalDateTime.now().plusDays(5).plusHours(2).withNano(0).format(FORMATTER);
    }
}
