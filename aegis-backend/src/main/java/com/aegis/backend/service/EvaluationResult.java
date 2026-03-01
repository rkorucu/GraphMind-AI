package com.aegis.backend.service;

import java.util.List;

/**
 * Result of running the evaluation module over a dataset.
 *
 * @param results              individual results for each question
 * @param averageConfidence    average confidence score across all questions
 * @param totalReflections     sum of all critic revisions across all questions
 * @param totalExecutionTimeMs total time taken to evaluate all questions
 */
public record EvaluationResult(
        List<QuestionResult> results,
        double averageConfidence,
        int totalReflections,
        long totalExecutionTimeMs) {

    public record QuestionResult(
            String question,
            String traceId,
            double confidence,
            int revisions,
            long durationMs) {
    }
}
