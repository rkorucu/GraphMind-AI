package com.aegis.backend.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Observability utility that traces graph node execution.
 *
 * <p>
 * Logs node name, sanitized input/output, duration, and inherits the
 * {@code traceId} from the SLF4J MDC (set by {@code TracingFilter}).
 */
@Component
public class NodeTracer {

    private static final Logger log = LoggerFactory.getLogger(NodeTracer.class);
    private static final int MAX_LOG_LENGTH = 500;

    /**
     * Logs a node execution event.
     *
     * @param nodeName  graph node name (e.g. "planner")
     * @param input     the input passed to the agent (sanitized before logging)
     * @param output    the raw output from the agent (sanitized before logging)
     * @param startTime when the node started executing
     */
    public void trace(String nodeName, String input, String output, Instant startTime) {
        long durationMs = Duration.between(startTime, Instant.now()).toMillis();
        String traceId = MDC.get("traceId");

        log.info("[TRACE] node={} | traceId={} | duration={}ms | inputLen={} | outputLen={}",
                nodeName, traceId != null ? traceId : "N/A",
                durationMs, input.length(), output.length());

        log.debug("[TRACE] node={} | input={}", nodeName, sanitize(input));
        log.debug("[TRACE] node={} | output={}", nodeName, sanitize(output));
    }

    /**
     * Logs a node execution failure.
     */
    public void traceError(String nodeName, String input, Instant startTime, Exception error) {
        long durationMs = Duration.between(startTime, Instant.now()).toMillis();
        String traceId = MDC.get("traceId");

        log.error("[TRACE] node={} | traceId={} | duration={}ms | FAILED: {}",
                nodeName, traceId != null ? traceId : "N/A",
                durationMs, error.getMessage());

        log.debug("[TRACE] node={} | failedInput={}", nodeName, sanitize(input));
    }

    /**
     * Sanitizes a string for safe logging — truncates long values and
     * strips newlines for single-line log output.
     */
    private String sanitize(String value) {
        if (value == null)
            return "[null]";
        String cleaned = value.replaceAll("[\\r\\n]+", " ").strip();
        if (cleaned.length() > MAX_LOG_LENGTH) {
            return cleaned.substring(0, MAX_LOG_LENGTH) + "… [truncated]";
        }
        return cleaned;
    }
}
