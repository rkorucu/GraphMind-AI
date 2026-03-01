package com.aegis.backend.service;

import com.aegis.backend.agent.AegisState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.NodeOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Orchestration service that invokes the compiled Aegis agent graph
 * and returns the final synthesised output.
 */
@Service
public class OrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(OrchestrationService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CompiledGraph<AegisState> compiledGraph;

    public OrchestrationService(CompiledGraph<AegisState> compiledGraph) {
        this.compiledGraph = compiledGraph;
    }

    /**
     * Executes the full agent pipeline for the given user query.
     *
     * @param userQuery the natural-language question or objective
     * @return the synthesiser's final JSON output (raw string)
     */
    public String execute(String userQuery) {
        return analyze(userQuery).analysis();
    }

    /**
     * Executes the full agent pipeline and returns a structured
     * {@link AnalysisResult} with the synthesiser output, extracted
     * confidence level, and revision count.
     *
     * @param userQuery the natural-language question or objective
     * @return structured analysis result
     */
    public AnalysisResult analyze(String userQuery) {
        return analyzeStreaming(userQuery, null);
    }

    /**
     * Executes the full agent pipeline, invoking an optional callback
     * each time a graph node completes. This enables SSE streaming.
     *
     * @param userQuery    the natural-language question or objective
     * @param nodeCallback optional callback receiving (nodeName, durationMs)
     * @return structured analysis result
     */
    public AnalysisResult analyzeStreaming(String userQuery,
            java.util.function.BiConsumer<String, Long> nodeCallback) {
        log.info("Starting orchestration for query: {}",
                userQuery.length() > 100 ? userQuery.substring(0, 100) + "…" : userQuery);

        try {
            AegisState finalState = null;

            for (NodeOutput<AegisState> nodeOutput : compiledGraph.stream(
                    Map.of(AegisState.USER_QUERY, userQuery))) {

                long timestamp = System.currentTimeMillis();
                finalState = nodeOutput.state();
                String nodeName = nodeOutput.node();
                log.debug("Completed node: {}", nodeName);

                if (nodeCallback != null && nodeName != null
                        && !nodeName.isBlank()
                        && !"__START__".equals(nodeName)
                        && !"__END__".equals(nodeName)) {
                    try {
                        nodeCallback.accept(nodeName, System.currentTimeMillis() - timestamp);
                    } catch (Exception cbErr) {
                        log.warn("Node callback failed for {}: {}", nodeName, cbErr.getMessage());
                    }
                }
            }

            if (finalState == null) {
                throw new IllegalStateException("Graph produced no output");
            }

            String synthesis = finalState.synthesizerOutput();
            double confidence = extractConfidence(synthesis);
            int revisions = finalState.revisionCount();

            log.info("Orchestration completed — revisions: {}, confidence: {}, output length: {} chars",
                    revisions, confidence, synthesis.length());

            return new AnalysisResult(synthesis, confidence, revisions);

        } catch (Exception e) {
            log.error("Orchestration failed for query: {}", userQuery, e);
            String causeMsg = (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
            throw new RuntimeException("Agent orchestration failed: " + causeMsg, e);
        }
    }

    /**
     * Extracts the confidence level from the synthesiser's JSON output.
     * Maps string confidence levels to numeric values, or reads a numeric
     * field if present. Defaults to {@code 0.5} on parse failure.
     */
    private double extractConfidence(String synthesizerJson) {
        try {
            JsonNode root = MAPPER.readTree(synthesizerJson);

            // Try numeric confidenceScore field first
            if (root.has("confidenceScore") && root.get("confidenceScore").isNumber()) {
                return root.get("confidenceScore").asDouble();
            }

            // Map string confidenceLevel to numeric
            if (root.has("confidenceLevel") && root.get("confidenceLevel").isTextual()) {
                return switch (root.get("confidenceLevel").asText().toUpperCase()) {
                    case "HIGH" -> 0.85;
                    case "MEDIUM" -> 0.60;
                    case "LOW" -> 0.35;
                    default -> 0.50;
                };
            }
        } catch (Exception e) {
            log.warn("Failed to extract confidence from synthesizer output", e);
        }
        return 0.50;
    }
}
