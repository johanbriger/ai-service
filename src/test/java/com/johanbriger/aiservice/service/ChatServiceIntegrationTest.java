package com.johanbriger.aiservice.service;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@WireMockTest(httpPort = 8089) // Startar WireMock på port 8089
public class ChatServiceIntegrationTest {

    @Autowired
    private ChatService chatService;

    // Denna metod skriver över URL:en i application.properties under testkörningen
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.api.url", () -> "http://localhost:8089");
        registry.add("ai.api.key", () -> "test-key"); // Fejk-nyckel
    }

    @Test
    void testChatSuccess() {
        // 1. Rigga WireMock (Stubbing)
        stubFor(post(urlEqualTo("/api/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"choices\":[{\"message\":{\"content\":\"Hallå där! Jag är en fejkad AI.\"}}]}")));

        // 2. Anropa den riktiga servicen
        String response = chatService.processChat("pirate", "Hej", "test-session-1");

        // 3. Verifiera resultatet
        assertNotNull(response);
        assertTrue(response.contains("fejkad AI"));
    }


    @Test
    void testRetryLogicWithWireMock() {
        String scenarioName = "Retry Scenario";

        // 1. Första anropet: Svara med 500 Server Error
        stubFor(post(urlEqualTo("/api/v1/chat/completions"))
                .inScenario(scenarioName)
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("First Failure"));

        // 2. Andra anropet: Svara med 500 Server Error igen
        stubFor(post(urlEqualTo("/api/v1/chat/completions"))
                .inScenario(scenarioName)
                .whenScenarioStateIs("First Failure")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("Second Failure"));

        // 3. Tredje anropet: Nu svarar vi med 200 OK!
        stubFor(post(urlEqualTo("/api/v1/chat/completions"))
                .inScenario(scenarioName)
                .whenScenarioStateIs("Second Failure")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"choices\":[{\"message\":{\"content\":\"Tredje gången gillt!\"}}]}")));

        // KÖR TESTET
        String response = chatService.processChat("pirate", "Hallå?", "retry-session");

        // VERIFIERA
        assertTrue(response.contains("Tredje gången gillt!"));

        // Verifiera att WireMock fick 3 anrop totalt
        verify(3, postRequestedFor(urlEqualTo("/api/v1/chat/completions")));
    }
}