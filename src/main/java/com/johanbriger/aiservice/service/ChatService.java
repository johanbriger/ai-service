package com.johanbriger.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johanbriger.aiservice.model.dto.LlmMessage;
import com.johanbriger.aiservice.model.dto.LlmRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, List<LlmMessage>> chatHistory = new ConcurrentHashMap<>();

    public ChatService(RestClient restClient) {
        this.restClient = restClient;
    }

    public String getSystemPrompt(String personality) {
        String baseInstructions = " Svara alltid på svenska.";

        // Logga här för att se vad Java faktiskt tar emot från React:
        System.out.println("DEBUG: Mottagen personlighet i Java: " + personality);

        return switch (personality.toLowerCase()) {
            // Ändra "helper" här så den matchar React-ID:t
            case "helper" ->
                    "Du är FunnyAI:s standard-assistent. Du är hjälpsam men älskar usla ordvitsar." + baseInstructions;

            case "sarcastic" ->
                    "Du är Sarkastiske Simon. Du svarar med ironi." + baseInstructions;

            case "chaos" ->
                    "Du är Kaos-Boten. Du är hyperaktiv och absurd." + baseInstructions;

            default -> {
                System.out.println("DEBUG: Hamnade i default! Använder pappa-humor.");
                yield "Du är FunnyAI:s standard-assistent med pappa-humor." + baseInstructions;
            }
        };
    }

    public String processChat(String personality, String message, String sessionId) {
        String id = (sessionId == null || sessionId.isEmpty()) ? "default-session" : sessionId;
        List<LlmMessage> history = chatHistory.computeIfAbsent(id, k -> new ArrayList<>());

        // 1. Kontrollera om vi redan har en system-prompt i början
        if (!history.isEmpty() && history.get(0).role().equals("system")) {
            // Uppdatera befintlig system-prompt till den nya personligheten
            history.set(0, new LlmMessage("system", getSystemPrompt(personality)));
        } else if (history.isEmpty()) {
            // Om listan är tom, lägg till den för första gången
            history.add(new LlmMessage("system", getSystemPrompt(personality)));
        } else {
            // Om listan inte är tom men saknar system-prompt (ovanligt), lägg till den längst fram
            history.add(0, new LlmMessage("system", getSystemPrompt(personality)));
        }

        history.add(new LlmMessage("user", message));
        LlmRequest llmRequest = new LlmRequest("openai/gpt-oss-120b:free", history);

        // Resiliens-logik (Retry) för RestClient
        int maxAttempts = 3;
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxAttempts) {
            try {
                // 4. Anrop med RestClient (Betydligt renare!)
                String responseJson = restClient.post()
                        .uri("/chat/completions")
                        .body(llmRequest)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (req, res) -> {
                            throw new RestClientResponseException("API misslyckades", res.getStatusCode(), res.getStatusText(), null, null, null);
                        })
                        .body(String.class);

                System.out.println("Svar från OpenRouter: " + responseJson);

                JsonNode root = objectMapper.readTree(responseJson);
                String aiAnswer = root.path("choices").get(0).path("message").path("content").asText();

                history.add(new LlmMessage("assistant", aiAnswer));
                return aiAnswer;

            } catch (Exception e) {
                attempt++;
                lastException = e;
                if (attempt < maxAttempts) {
                    long sleepTime = (long) Math.pow(2, attempt) * 1000; // Exponential Backoff (2s, 4s...)
                    System.out.println("Försök " + attempt + " misslyckades. Testar igen om " + sleepTime + "ms...");
                    try { Thread.sleep(sleepTime); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }

        throw new RuntimeException("AI-tjänsten svarar inte efter " + maxAttempts + " försök. Senaste fel: " + lastException.getMessage());
    }
}