package com.johanbriger.aiservice.model.dto;

public record ErrorResponse(
        String error,
        String message,
        long timestamp
) {}
