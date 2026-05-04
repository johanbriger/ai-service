package com.johanbriger.aiservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "AI_API_KEY=test-key",
        "AI_BASE_URL=http://localhost:8080",
        "OPENROUTER_API_KEY=test-key"
})
class AiServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
