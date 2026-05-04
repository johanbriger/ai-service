package com.johanbriger.aiservice.model.dto;

public record LlmRequest(String model, java.util.List<LlmMessage> messages) {}
