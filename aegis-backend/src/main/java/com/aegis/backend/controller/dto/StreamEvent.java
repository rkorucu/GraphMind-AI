package com.aegis.backend.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * SSE event payload sent during streaming analysis.
 *
 * <p>
 * Two event types:
 * <ul>
 * <li><b>Step event</b> — emitted when a graph node completes
 * ({@code node}, {@code status}, {@code durationMs},
 * {@code outputPreview})</li>
 * <li><b>Done event</b> — emitted at the end with full result
 * ({@code node = "__DONE__"}, {@code analysis}, {@code confidence},
 * {@code traceId})</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StreamEvent(
        String node,
        String status,
        Long durationMs,
        String outputPreview,
        // final-result fields (only on __DONE__ event)
        String analysis,
        Double confidence,
        String traceId) {

    /** Create a step-completed event. */
    public static StreamEvent step(String node, long durationMs, String outputPreview) {
        String preview = outputPreview;
        if (preview != null && preview.length() > 300) {
            preview = preview.substring(0, 300) + "…";
        }
        return new StreamEvent(node, "done", durationMs, preview, null, null, null);
    }

    /** Create the final done event. */
    public static StreamEvent done(String analysis, double confidence, String traceId) {
        return new StreamEvent("__DONE__", "complete", null, null, analysis, confidence, traceId);
    }
}
