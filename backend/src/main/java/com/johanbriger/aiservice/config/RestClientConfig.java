package com.johanbriger.aiservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Configuration
public class RestClientConfig {

    @Value("${AI_BASE_URL}")
    private String baseUrl;

    @Value("${AI_API_KEY}")
    private String apiKey;

    @Bean
    public RestClient openRouterClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
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