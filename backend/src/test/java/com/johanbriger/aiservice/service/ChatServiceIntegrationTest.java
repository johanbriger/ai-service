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
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnableRetry // SÄKERSTÄLL: Att Spring Retry är aktivt under våra integrationstester!
public class ChatServiceIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private ChatService chatService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("AI_BASE_URL", wireMockServer::baseUrl);
        registry.add("AI_API_KEY", () -> "test-key");
        registry.add("OPENROUTER_API_KEY", () -> "test-key");
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public RestClient testRestClient(RestClient.Builder builder,
                                         @Value("${AI_BASE_URL}") String wiremockBaseUrl) {

            HttpClient httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

            return builder
                    .baseUrl(wiremockBaseUrl)
                    .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                    // VIKTIGT: Vi måste återskapa felhanteringen här så att test-klienten kastar rätt sorts Exceptions!
                    .defaultStatusHandler(
                            statusCode -> statusCode.is4xxClientError(),
                            (request, response) -> {
                                throw HttpClientErrorException.create(
                                        response.getStatusCode(),
                                        response.getStatusText(),
                                        response.getHeaders(),
                                        response.getBody().readAllBytes(),
                                        null
                                );
                            }
                    )
                    .defaultStatusHandler(
                            statusCode -> statusCode.is5xxServerError(),
                            (request, response) -> {
                                throw HttpServerErrorException.create(
                                        response.getStatusCode(),
                                        response.getStatusText(),
                                        response.getHeaders(),
                                        response.getBody().readAllBytes(),
                                        null
                                );
                            }
                    )
                    .build();
        }
    }

    @BeforeEach
    void setup() {
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

        // Första anropet: Returnera 500 Server Error
        stubFor(post(urlEqualTo("/chat/completions"))
                .inScenario(scenario)
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("Failed once"));

        // Andra anropet: Nu lyckas det!
        stubFor(post(urlEqualTo("/chat/completions"))
                .inScenario(scenario)
                .whenScenarioStateIs("Failed once")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"choices\":[{\"message\":{\"content\":\"Nu funkade det!\"}}]}")));

        String response = chatService.processChat("pirate", "Hej", "retry-session");
        assertTrue(response.contains("Nu funkade det!"));

        // Verifiera att Wiremock faktiskt tog emot exakt 2 anrop (ett som misslyckades och ett som lyckades)
        verify(2, postRequestedFor(urlEqualTo("/chat/completions")));
    }
}