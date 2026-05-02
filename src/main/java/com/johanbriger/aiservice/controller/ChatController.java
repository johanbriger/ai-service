package com.johanbriger.aiservice.controller;


import com.johanbriger.aiservice.model.dto.ChatRequest;
import com.johanbriger.aiservice.model.dto.ChatResponse;
import com.johanbriger.aiservice.service.ChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest chatRequest) {

        String result = chatService.processChat(
                chatRequest.getPersonality(),
                chatRequest.getMessage(),
                chatRequest.getSessionId()
        );

        return new ChatResponse(result);

    }

}
