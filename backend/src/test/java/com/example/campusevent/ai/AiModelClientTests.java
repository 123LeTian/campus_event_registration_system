package com.example.campusevent.ai;

import com.example.campusevent.ai.client.AiModelClient;
import com.example.campusevent.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiModelClientTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void missingApiKeyWithoutMockReturnsBusinessError() {
        AiModelClient client = new AiModelClient(objectMapper, "", "test-model", "http://127.0.0.1:1/v1/chat/completions", false, 1000);

        assertThatThrownBy(() -> client.generate("system", "user"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 服务未配置，请手工填写");
    }

    @Test
    void timeoutReturnsBusinessError() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            try {
                Thread.sleep(1500);
                byte[] body = "{\"choices\":[{\"message\":{\"content\":\"{}\"}}]}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
            AiModelClient client = new AiModelClient(objectMapper, "test-key", "test-model", url, false, 1000);

            assertThatThrownBy(() -> client.generate("system", "user"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("AI 生成超时，请稍后重试或手工填写");
        } finally {
            server.stop(0);
        }
    }
}
