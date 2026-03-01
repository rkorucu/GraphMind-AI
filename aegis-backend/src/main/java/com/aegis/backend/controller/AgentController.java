package com.aegis.backend.controller;

import com.aegis.backend.controller.dto.AnalyzeRequest;
import com.aegis.backend.controller.dto.AnalyzeResponse;
import com.aegis.backend.controller.dto.StreamEvent;
import com.aegis.backend.service.AnalysisResult;
import com.aegis.backend.service.EvaluationResult;
import com.aegis.backend.service.EvaluationService;
import com.aegis.backend.service.OrchestrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST controller that exposes the multi-agent orchestration pipeline.
 */
@RestController
@RequestMapping("/api/agent")
@CrossOrigin(origins = "*")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OrchestrationService orchestrationService;
    private final EvaluationService evaluationService;
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    public AgentController(OrchestrationService orchestrationService, EvaluationService evaluationService) {
        this.orchestrationService = orchestrationService;
        this.evaluationService = evaluationService;
    }

    /**
     * Executes the evaluation module against the stored dataset and returns
     * aggregated metrics.
     *
     * @return Evaluation dataset results
     */
    @GetMapping("/eval")
    public ResponseEntity<EvaluationResult> evaluate() {
        return ResponseEntity.ok(evaluationService.runEvaluation());
    }

    /**
     * Executes the full agent pipeline (legacy endpoint).
     *
     * @param request body containing a {@code query} field
     * @return the synthesiser's final JSON output
     */
    @PostMapping("/execute")
    public ResponseEntity<String> execute(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"'query' field is required\"}");
        }

        String result = orchestrationService.execute(query);
        return ResponseEntity.ok(result);
    }

    /**
     * Analyses a question through the full multi-agent pipeline and returns
     * a structured response with analysis, confidence score, and trace ID.
     *
     * @param request body containing a {@code question} field
     * @return structured analysis result
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody AnalyzeRequest request) {
        if (request == null || !request.isValid()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "'question' field is required"));
        }

        AnalysisResult result = orchestrationService.analyze(request.question());
        String traceId = MDC.get("traceId");

        return ResponseEntity.ok(new AnalyzeResponse(
                result.analysis(),
                result.confidence(),
                traceId != null ? traceId : "N/A"));
    }

    /**
     * Streams the agent pipeline execution via Server-Sent Events.
     * Emits one event per graph node completion, then a final __DONE__ event.
     *
     * @param request body containing a {@code question} field
     * @return SSE stream
     */
    @PostMapping(value = "/analyze-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeStream(@RequestBody AnalyzeRequest request) {
        // 5-minute timeout for long-running pipelines
        SseEmitter emitter = new SseEmitter(300_000L);

        if (request == null || !request.isValid()) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\": \"'question' field is required\"}"));
                emitter.complete();
            } catch (Exception ignored) {
            }
            return emitter;
        }

        String traceId = MDC.get("traceId");

        sseExecutor.execute(() -> {
            // Propagate traceId to background thread
            if (traceId != null) {
                MDC.put("traceId", traceId);
            }
            try {
                AnalysisResult result = orchestrationService.analyzeStreaming(
                        request.question(),
                        (nodeName, durationMs) -> {
                            try {
                                StreamEvent event = StreamEvent.step(nodeName, durationMs, null);
                                emitter.send(SseEmitter.event()
                                        .name("step")
                                        .data(MAPPER.writeValueAsString(event)));
                            } catch (Exception e) {
                                log.warn("Failed to send SSE step event for {}: {}",
                                        nodeName, e.getMessage());
                            }
                        });

                String currentTraceId = MDC.get("traceId");
                StreamEvent doneEvent = StreamEvent.done(
                        result.analysis(),
                        result.confidence(),
                        currentTraceId != null ? currentTraceId : "N/A");

                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(MAPPER.writeValueAsString(doneEvent)));
                emitter.complete();

            } catch (Exception e) {
                log.error("SSE streaming failed: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}"));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            } finally {
                MDC.clear();
            }
        });

        return emitter;
    }
}
