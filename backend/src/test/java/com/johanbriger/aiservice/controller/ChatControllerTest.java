package com.johanbriger.aiservice.controller;

import com.johanbriger.aiservice.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
public class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @Test
    public void testChatEndpointSuccess() throws Exception {

        when(chatService.processChat(anyString(), anyString(), anyString()))
                .thenReturn("Hej på dig!");


        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personality\":\"helper\", \"message\":\"Hej\", \"sessionId\":\"123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Hej på dig!"));
    }
}