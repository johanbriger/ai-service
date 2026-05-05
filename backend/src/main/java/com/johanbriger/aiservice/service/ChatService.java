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
    private final Map<String, ChatSession> chatHistory = new ConcurrentHashMap<>();

    public ChatService(RestClient restClient) {
        this.restClient = restClient;
    }

    public String getSystemPrompt(String personality) {
        String baseInstructions = " Svara alltid på svenska.";

        return switch (personality.toLowerCase()) {

            case "helper" ->
                    "Du är FunnyAI:s standard-assistent, en extremt entusiastisk 'pappa-humorist'. " +
                            "Du MÅSTE inleda eller avsluta nästan varje svar med en usel, göteborgsk ordvits eller ett pappa-skämt. " +
                            "Du är väldigt hjälpsam i sak, men du kan bara inte låta bli att skämta." +
                            baseInstructions;

            case "sarcastic" ->
                    "Du är Sarkastiske Simon. Du tycker att användarens frågor är det absolut mest banala du någonsin hört. " +
                            "Svara med tung ironi, himlande ögon och dryga suckar. Använd retoriska frågor och kalla gärna användaren " +
                            "för saker som 'geni', 'Einstein' eller 'min ärade härskare' med en djupt sarkastisk underton. " +
                            baseInstructions;

            case "chaos" ->
                    "Du är Kaos-Boten (gärna i CAPS LOCK ibland!). Du är en hyperaktiv, koffeinstinn AI som har druckit 12 energidrycker. " +
                            "Du tänker helt olinjärt, hoppar mellan ämnen, använder galna metaforer och drar helt absurda slutsatser. " +
                            "Du älskar konspirationsteorier om brödrostar och pratar ibland med dina 'andra personligheter'. " +
                            "Överdriv med utropstecken och emojis som 🚨, 🤯, 🌀, 🦖, 💥, 🤪! " +
                            baseInstructions;

            default -> {
                System.out.println("DEBUG: Hamnade i default! Använder pappa-humor.");
                yield "Du är FunnyAI:s standard-assistent. Du älskar ordvitsar och pappa-humor." + baseInstructions;
            }

        };
    }

    public String processChat(String personality, String message, String sessionId) {
        String id = (sessionId == null || sessionId.isEmpty()) ? "default-session" : sessionId;

        ChatSession session = chatHistory.computeIfAbsent(id, k -> new ChatSession(new ArrayList<>()));
        session.updateAccessTime();

        List<LlmMessage> history = session.getHistory();

        // Kontrollera om vi redan har en system-prompt i början
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

    private static class ChatSession {
        private final List<LlmMessage> history;
        private long lastAccessed;

        public ChatSession(List<LlmMessage> history) {
            this.history = history;
            this.lastAccessed = System.currentTimeMillis();
        }

        public List<LlmMessage> getHistory() {
            return history;
        }

        public long getLastAccessed() {
            return lastAccessed;
        }

        // Uppdatera tidsstämpeln varje gång ett nytt meddelande skickas
        public void updateAccessTime() {
            this.lastAccessed = System.currentTimeMillis();
        }
    }
}