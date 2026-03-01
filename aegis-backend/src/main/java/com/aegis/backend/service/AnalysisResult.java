package com.aegis.backend.service;

/**
 * Structured result from the orchestration pipeline.
 *
 * @param analysis      the synthesiser's final output (raw JSON string)
 * @param confidence    confidence level extracted from the synthesiser output
 *                      (0.0 – 1.0)
 * @param revisionCount number of critic revision loops that occurred
 */
public record AnalysisResult(String analysis, double confidence, int revisionCount) {
}
