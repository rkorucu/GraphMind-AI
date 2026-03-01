package com.aegis.backend.controller.dto;

/**
 * Request body for {@code POST /analyze}.
 */
public record AnalyzeRequest(String question) {

    public boolean isValid() {
        return question != null && !question.isBlank();
    }
}
