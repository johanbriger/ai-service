package com.johanbriger.aiservice.service;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class ChatServiceIntegrationTest {

    // 1. Starta WireMock på en dynamisk port
    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private ChatService chatService;

    // 2. Mappa porten till din applikations properties
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("AI_BASE_URL", wireMockServer::baseUrl);
        registry.add("AI_API_KEY", () -> "test-key");
    }

    // 3. LOGISK FIX: Skapa en test-klient som bara lever i detta test
    @TestConfiguration
    static class TestConfig {

        // Vi hämtar AI_BASE_URL som vi satte i configureProperties ovan
        @Bean
        @Primary
        public RestClient testRestClient(RestClient.Builder builder,
                                         @Value("${AI_BASE_URL}") String wiremockBaseUrl) {

            HttpClient httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

            return builder
                    .baseUrl(wiremockBaseUrl) // KRITISKT: Lägg till schemat (http://localhost:port)
                    .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                    .build();
        }
    }

    @BeforeEach
    void setup() {
        // Berätta för stubFor() vilken port den ska prata med
        WireMock.configureFor("localhost", wireMockServer.getPort());
    }

    @Test
    void testChatSuccess() {
        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"choices\":[{\"message\":{\"content\":\"Hallå där!\"}}]}")));

        String response = chatService.processChat("pirate", "Hej", "session-1");
        assertTrue(response.contains("Hallå där"));
    }

    @Test
    void testRetryLogic() {
        String scenario = "RetryScenario";

        stubFor(post(urlEqualTo("/chat/completions"))
                .inScenario(scenario)
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("Failed once"));

        stubFor(post(urlEqualTo("/chat/completions"))
                .inScenario(scenario)
                .whenScenarioStateIs("Failed once")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"choices\":[{\"message\":{\"content\":\"Nu funkade det!\"}}]}")));

        String response = chatService.processChat("pirate", "Hej", "retry-session");
        assertTrue(response.contains("Nu funkade det!"));

        verify(2, postRequestedFor(urlEqualTo("/chat/completions")));
    }
}