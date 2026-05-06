package com.johanbriger.aiservice.exception;

import com.johanbriger.aiservice.model.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. Hantera klientfel från OpenRouter (t.ex. 429 Rate Limits eller 400 Bad Request)
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpClientError(HttpClientErrorException ex) {
        HttpStatus status = (HttpStatus) ex.getStatusCode();
        String errorCode = "EXTERNAL_CLIENT_ERROR";

        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            errorCode = "RATE_LIMIT_EXCEEDED";
        }

        ErrorResponse error = new ErrorResponse(
                errorCode,
                "Anropet mot OpenRouter misslyckades: " + ex.getStatusText(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, status);
    }

    // 2. Hantera serverfel från OpenRouter efter att alla retries i backend har misslyckats
    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpServerError(HttpServerErrorException ex) {
        HttpStatus status = (HttpStatus) ex.getStatusCode();
        ErrorResponse error = new ErrorResponse(
                "EXTERNAL_SERVER_ERROR",
                "Den externa AI-tjänsten är tillfälligt överbelastad. Försök igen om en stund.",
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, status);
    }

    // 3. Hantera nätverks-timeouts (om anslutningen dör helt)
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleNetworkTimeout(ResourceAccessException ex) {
        ErrorResponse error = new ErrorResponse(
                "NETWORK_TIMEOUT",
                "Kunde inte etablera kontakt med AI-tjänsten (timeout).",
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.GATEWAY_TIMEOUT);
    }

    // 4. Klassiska valideringsfel och ogiltiga lokala parametrar
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
                "BAD_REQUEST",
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // 5. Global fallback för oväntade interna serverfel
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        ErrorResponse error = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}