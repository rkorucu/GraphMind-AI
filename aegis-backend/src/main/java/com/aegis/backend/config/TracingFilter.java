package com.aegis.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that assigns a unique {@code traceId} to every incoming
 * HTTP request and places it in the SLF4J MDC so it appears in all log
 * lines for that request.
 *
 * <p>
 * If the caller sends an {@code X-Trace-Id} header, that value is reused;
 * otherwise a new UUID is generated. The trace ID is also returned as a
 * response header.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TracingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TracingFilter.class);

    public static final String MDC_TRACE_ID = "traceId";
    public static final String HEADER_NAME = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String traceId = request.getHeader(HEADER_NAME);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        MDC.put(MDC_TRACE_ID, traceId);
        response.setHeader(HEADER_NAME, traceId);

        try {
            log.info("→ {} {} [traceId={}]", request.getMethod(), request.getRequestURI(), traceId);
            filterChain.doFilter(request, response);
        } finally {
            log.info("← {} {} [status={}]", request.getMethod(), request.getRequestURI(), response.getStatus());
            MDC.remove(MDC_TRACE_ID);
        }
    }
}
