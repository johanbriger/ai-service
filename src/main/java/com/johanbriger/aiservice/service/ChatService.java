package com.johanbriger.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johanbriger.aiservice.model.dto.LlmMessage;
import com.johanbriger.aiservice.model.dto.LlmRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    @Value("${ai.api.key}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, List<LlmMessage>> chatHistory = new ConcurrentHashMap<>();

    public ChatService(WebClient webClient) {
        this.webClient = webClient;
    }

    public String getSystemPrompt(String personality) {
        return switch (personality.toLowerCase()) {
            case "pirate" -> "Du är en barsk pirat. Svara alltid med pirat-slang och avsluta med 'Arrr!'";
            case "coder" -> "Du är en hjälpsam senior java-utvecklare. Ge korta, koncisa kodexempel.";
            default -> "Du är en hjälpsam assistent.";
        };
    }

    public String processChat(String personality, String message, String sessionId) {
        // 1. Hantera sessionId (skapa ett standard-id om inget skickas med)
        String id = (sessionId == null || sessionId.isEmpty()) ? "default-session" : sessionId;

        // 2. Hämta tidigare historik eller skapa en ny lista om det är en ny session
        List<LlmMessage> history = chatHistory.computeIfAbsent(id, k -> new ArrayList<>());

        // 3. Om sessionen är helt ny (listan är tom), börja med System Prompt
        if (history.isEmpty()) {
            history.add(new LlmMessage("system", getSystemPrompt(personality)));
        }

        // 4. Lägg till användarens nya meddelande i historiken
        history.add(new LlmMessage("user", message));

        // 5. Skapa requesten med HELA historiken
        LlmRequest llmRequest = new LlmRequest("openrouter/auto", history);

        try {
            String responseJson = webClient.post()
                    .uri("/api/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", "http://localhost:8080")
                    .bodyValue(llmRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                    .filter(this::isRetryableException)
                                    .doAfterRetry(retrySignal ->
                                            System.out.println("Försök misslyckades. Försök igen " + (retrySignal.totalRetries() + 1)))
                    )
                    .block();

            String trimmedResponse = (responseJson != null) ? responseJson.trim() : "";

            if (trimmedResponse.startsWith("{")) {
                JsonNode root = objectMapper.readTree(trimmedResponse);
                String aiAnswer = root.path("choices").get(0).path("message").path("content").asText();

                // 6. SPARA AI:ns svar i historiken så den minns det till nästa gång!
                history.add(new LlmMessage("assistant", aiAnswer));

                return aiAnswer;
            }
            return "Oväntat format: " + trimmedResponse;

        } catch (Exception e) {
            throw new RuntimeException("AI-tjänsten svarar inte efter flera försök: " + e.getMessage());
        }
    }

    // Hjälpmetod
    private boolean isRetryableException(Throwable error) {

        return error instanceof org.springframework.web.reactive.function.client.WebClientResponseException e &&
                (e.getStatusCode().is5xxServerError() || e.getStatusCode().value() == 429);
    }
}