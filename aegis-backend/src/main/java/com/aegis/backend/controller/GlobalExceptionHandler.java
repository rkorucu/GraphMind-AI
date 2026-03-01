package com.aegis.backend.controller;

import com.aegis.backend.agent.AgentValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Global exception handler for REST endpoints.
 * Intercepts specific unhandled exceptions and standardizes
 * the JSON error responses sent to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles schema validation failures from the LLM responses.
     */
    @ExceptionHandler(AgentValidationException.class)
    public ResponseEntity<Map<String, String>> handleAgentValidationException(AgentValidationException ex) {
        log.warn("Agent validation failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of(
                        "error", "Agent output validation failed",
                        "detail", ex.getValidationResult().errorSummary()));
    }

    /**
     * Handles bad requests (missing/invalid fields in controller).
     */
    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles all other uncaught server errors (e.g. missing API keys, LangChain4j
     * failures).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Unhandled internal server error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error: " + ex.getMessage()));
    }
}
