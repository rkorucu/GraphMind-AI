package com.aegis.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runs the multi-agent pipeline against a standardized dataset of questions
 * to evaluate performance, accuracy, and reflection rates.
 */
@Service
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DATASET_PATH = "dataset.json";

    private final OrchestrationService orchestrationService;

    public EvaluationService(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    /**
     * Runs the evaluation dataset through the agent pipeline.
     */
    public EvaluationResult runEvaluation() {
        List<String> questions = loadDataset();
        if (questions.isEmpty()) {
            throw new IllegalStateException("Evaluation dataset is empty or could not be loaded");
        }

        log.info("Starting evaluation run for {} questions", questions.size());

        long globalStartTime = System.currentTimeMillis();
        List<EvaluationResult.QuestionResult> results = new ArrayList<>();
        double totalConfidence = 0.0;
        int totalRevisions = 0;

        for (int i = 0; i < questions.size(); i++) {
            String question = questions.get(i);
            String traceId = UUID.randomUUID().toString();
            // Optional: simulate unique trace per question
            MDC.put("traceId", "eval-" + traceId.substring(0, 8));

            log.info("Evaluating Question {}/{}: {}", i + 1, questions.size(), question);

            long startTime = System.currentTimeMillis();
            try {
                // Run the full pipeline synchronously for evaluation
                AnalysisResult analysis = orchestrationService.analyze(question);
                long durationMs = System.currentTimeMillis() - startTime;

                results.add(new EvaluationResult.QuestionResult(
                        question, MDC.get("traceId"), analysis.confidence(), analysis.revisionCount(), durationMs));

                totalConfidence += analysis.confidence();
                totalRevisions += analysis.revisionCount();

                log.info("Finished Question {} - Revisions: {}, Duration: {}ms",
                        i + 1, analysis.revisionCount(), durationMs);
            } catch (Exception e) {
                log.error("Failed evaluating question: {}", question, e);
                results.add(new EvaluationResult.QuestionResult(
                        question, MDC.get("traceId"), 0.0, 0, System.currentTimeMillis() - startTime));
            } finally {
                MDC.remove("traceId");
            }
        }

        long totalExecutionTimeMs = System.currentTimeMillis() - globalStartTime;
        double averageConfidence = totalConfidence / questions.size();

        log.info("Evaluation complete. Avg Confidence: {}, Total Revisions: {}, Total Time: {}ms",
                averageConfidence, totalRevisions, totalExecutionTimeMs);

        return new EvaluationResult(results, averageConfidence, totalRevisions, totalExecutionTimeMs);
    }

    private List<String> loadDataset() {
        try {
            ClassPathResource resource = new ClassPathResource(DATASET_PATH);
            try (InputStream is = resource.getInputStream()) {
                return MAPPER.readValue(is, new TypeReference<List<String>>() {
                });
            }
        } catch (Exception e) {
            log.error("Failed to load evaluation dataset from {}", DATASET_PATH, e);
            return List.of();
        }
    }
}
