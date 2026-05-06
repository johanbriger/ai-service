package com.johanbriger.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johanbriger.aiservice.model.dto.LlmMessage;
import com.johanbriger.aiservice.model.dto.LlmRequest;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

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

    // Kör endast retry på serverfel (5xx) och nätverksanslutnings-timeouts.
    // Vi undviker medvetet klientfel (4xx) för att inte skapa onödiga anrop vid obehörighet eller valideringsfel.
    @Retryable(
            retryFor = { HttpServerErrorException.class, ResourceAccessException.class },
            maxAttempts = 3,
            backoff = @Backoff(
                    delay = 2000,      // Vänta 2 sekunder vid första felet
                    multiplier = 2,    // Dubbla väntetiden vid nästa försök (4 sekunder)
                    random = true      // Aktiverar slumpmässig Jitter för att sprida ut lasten
            )
    )
    public String processChat(String personality, String message, String sessionId) {
        String id = (sessionId == null || sessionId.isEmpty()) ? "default-session" : sessionId;

        ChatSession session = chatHistory.computeIfAbsent(id, k -> new ChatSession(new ArrayList<>()));
        session.updateAccessTime();

        List<LlmMessage> history = session.getHistory();

        // Kontrollera system-prompt i början
        if (!history.isEmpty() && history.get(0).role().equals("system")) {
            history.set(0, new LlmMessage("system", getSystemPrompt(personality)));
        } else if (history.isEmpty()) {
            history.add(new LlmMessage("system", getSystemPrompt(personality)));
        } else {
            history.add(0, new LlmMessage("system", getSystemPrompt(personality)));
        }

        history.add(new LlmMessage("user", message));
        LlmRequest llmRequest = new LlmRequest("openai/gpt-oss-120b:free", history);

        try {
            // Mycket renare anrop utan lokal while-loop eller manuell statuskodsinspektion
            String responseJson = restClient.post()
                    .uri("/chat/completions")
                    .body(llmRequest)
                    .retrieve()
                    .body(String.class);

            System.out.println("Svar från OpenRouter: " + responseJson);

            JsonNode root = objectMapper.readTree(responseJson);
            String aiAnswer = root.path("choices").get(0).path("message").path("content").asText();

            history.add(new LlmMessage("assistant", aiAnswer));
            return aiAnswer;

        } catch (HttpClientErrorException ex) {
            // Om API:et kastar ett klientfel (t.ex. 400 Bad Request eller 429 Too Many Requests),
            // rensar vi bort användarens senaste ogiltiga meddelande så att chatten inte hänger sig i framtida anrop.
            if (!history.isEmpty()) {
                history.remove(history.size() - 1);
            }
            throw ex;
        } catch (Exception e) {
            // Om det är JSON-parsningsfel eller liknande internt fel
            throw new RuntimeException("Kunde inte bearbeta svaret från AI-tjänsten: " + e.getMessage(), e);
        }
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

        public void updateAccessTime() {
            this.lastAccessed = System.currentTimeMillis();
        }
    }
}