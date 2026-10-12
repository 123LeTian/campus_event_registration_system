package com.example.campusevent.common;

import com.example.campusevent.common.api.ErrorCode;
import com.example.campusevent.common.exception.GlobalExceptionHandler;
import com.example.campusevent.common.security.JwtTokenProvider;
import com.example.campusevent.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExceptionHandlingTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserMapper userMapper;

    @Test
    void malformedJsonReturnsUnifiedValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR))
                .andExpect(jsonPath("$.message").value("请求体格式错误"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void typeMismatchReturnsUnifiedValidationError() throws Exception {
        mockMvc.perform(get("/api/statistics/events")
                        .param("page", "not-a-number")
                        .header("Authorization", bearer("admin")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR))
                .andExpect(jsonPath("$.message").value("page: 请求参数类型错误"));
    }

    @Test
    void unknownApiReturnsUnifiedNotFoundError() throws Exception {
        mockMvc.perform(get("/api/not-exists").header("Authorization", bearer("admin")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND))
                .andExpect(jsonPath("$.message").value("资源不存在"));
    }

    @Test
    void aiConfigurationFailureUsesUnifiedInternalErrorCode() throws Exception {
        var client = new com.example.campusevent.ai.client.AiModelClient(
                new com.fasterxml.jackson.databind.ObjectMapper(),
                "",
                "test-model",
                "http://127.0.0.1:1/v1/chat/completions",
                false,
                1000
        );

        try {
            client.generate("system", "user");
        } catch (com.example.campusevent.common.exception.BusinessException ex) {
            assertThat(ex.getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
            assertThat(ex.getMessage()).isEqualTo("AI 服务未配置，请手工填写");
            return;
        }
        throw new AssertionError("Expected BusinessException");
    }

    @Test
    void duplicateKeyResponseDoesNotExposeSqlDetails() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        DuplicateKeyException exception = new DuplicateKeyException("Duplicate entry 'studentA' for key 'uk_user_username'; SQL statement: insert into user");

        var response = handler.handleDuplicateKeyException(exception, request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("数据已存在或重复提交");
        assertThat(response.getBody().message()).doesNotContainIgnoringCase("sql");
        assertThat(response.getBody().message()).doesNotContain("uk_user_username");
    }

    private String bearer(String username) {
        return "Bearer " + jwtTokenProvider.createToken(userMapper.findByUsername(username));
    }
}
