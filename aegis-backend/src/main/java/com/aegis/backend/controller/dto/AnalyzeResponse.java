package com.aegis.backend.controller.dto;

/**
 * Response body for {@code POST /analyze}.
 */
public record AnalyzeResponse(String analysis, double confidence, String traceId) {
}
