package com.aegis.backend.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.EdgeAction;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Wires the LangGraph4j {@link StateGraph} that orchestrates all Aegis agents.
 *
 * <p>
 * Each node wrapper:
 * <ol>
 * <li>Traces execution via {@link NodeTracer}</li>
 * <li>Validates output via {@link AgentOutputValidator}</li>
 * <li>Propagates state on success, throws on failure</li>
 * </ol>
 *
 * <p>
 * Flow:
 * 
 * <pre>
 *   START → planner → research → risk → critic
 *                                         ↓
 *                               ┌─────────┴─────────┐
 *                          APPROVE / max-loops   REVISE / REJECT
 *                               ↓                    ↓
 *                          synthesizer           planner (loop)
 *                               ↓
 *                              END
 * </pre>
 */
@Configuration
public class AgentGraphConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentGraphConfig.class);
    private static final int MAX_REVISIONS = 2;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Node names ──────────────────────────────────────────────────────
    private static final String PLANNER = "planner";
    private static final String RESEARCH = "research";
    private static final String RISK = "risk";
    private static final String CRITIC = "critic";
    private static final String SYNTHESIZER = "synthesizer";

    @Bean
    public CompiledGraph<AegisState> aegisCompiledGraph(
            PlannerAgent plannerAgent,
            ResearchAgent researchAgent,
            RiskAgent riskAgent,
            CriticAgent criticAgent,
            SynthesizerAgent synthesizerAgent,
            AgentOutputValidator validator,
            NodeTracer tracer) throws Exception {

        log.info("Building Aegis agent graph …");

        var graph = new StateGraph<>(AegisState.SCHEMA, AegisState::new)

                // Nodes
                .addNode(PLANNER, node_async(plannerNode(plannerAgent, validator, tracer)))
                .addNode(RESEARCH, node_async(researchNode(researchAgent, validator, tracer)))
                .addNode(RISK, node_async(riskNode(riskAgent, validator, tracer)))
                .addNode(CRITIC, node_async(criticNode(criticAgent, validator, tracer)))
                .addNode(SYNTHESIZER, node_async(synthesizerNode(synthesizerAgent, validator, tracer)))

                // Linear edges
                .addEdge(START, PLANNER)
                .addEdge(PLANNER, RESEARCH)
                .addEdge(RESEARCH, RISK)
                .addEdge(RISK, CRITIC)

                // Conditional edge: critic decides next step
                .addConditionalEdges(CRITIC, edge_async(criticRouter()),
                        Map.of(
                                SYNTHESIZER, SYNTHESIZER,
                                PLANNER, PLANNER))

                // Terminal edge
                .addEdge(SYNTHESIZER, END);

        CompiledGraph<AegisState> compiled = graph.compile();
        log.info("Aegis agent graph compiled successfully");
        return compiled;
    }

    // ── Routing logic ───────────────────────────────────────────────────

    private EdgeAction<AegisState> criticRouter() {
        return state -> {
            String verdict = state.criticVerdict().toUpperCase();
            int revisions = state.revisionCount();

            if ("APPROVE".equals(verdict) || revisions >= MAX_REVISIONS) {
                if (revisions >= MAX_REVISIONS && !"APPROVE".equals(verdict)) {
                    log.warn("Max revision limit ({}) reached — forcing synthesis", MAX_REVISIONS);
                }
                return SYNTHESIZER;
            }

            log.info("Critic verdict: {} — routing back to planner (revision #{})", verdict, revisions + 1);
            return PLANNER;
        };
    }

    // ── Node actions ────────────────────────────────────────────────────

    private NodeAction<AegisState> plannerNode(PlannerAgent agent,
            AgentOutputValidator validator,
            NodeTracer tracer) {
        return state -> {
            String query = state.userQuery();
            if (state.revisionCount() > 0) {
                query = """
                        REVISION REQUEST (attempt %d):
                        Original query: %s
                        Previous critic feedback: %s
                        Previous plan: %s
                        Please revise the plan addressing the critic's concerns.
                        """.formatted(
                        state.revisionCount(),
                        state.userQuery(),
                        state.criticOutput(),
                        state.plannerOutput());
            }

            Instant start = Instant.now();
            try {
                String output = agent.call(query);
                tracer.trace(PLANNER, query, output, start);
                assertValid(validator.validatePlanner(output), output);
                return Map.of(AegisState.PLANNER_OUTPUT, output);
            } catch (Exception e) {
                tracer.traceError(PLANNER, query, start, e);
                throw e;
            }
        };
    }

    private NodeAction<AegisState> researchNode(ResearchAgent agent,
            AgentOutputValidator validator,
            NodeTracer tracer) {
        return state -> {
            String input = """
                    User query: %s
                    Execution plan: %s
                    Conduct research based on the above plan.
                    """.formatted(state.userQuery(), state.plannerOutput());

            Instant start = Instant.now();
            try {
                String output = agent.call(input);
                tracer.trace(RESEARCH, input, output, start);
                assertValid(validator.validateResearch(output), output);
                return Map.of(AegisState.RESEARCH_OUTPUT, output);
            } catch (Exception e) {
                tracer.traceError(RESEARCH, input, start, e);
                throw e;
            }
        };
    }

    private NodeAction<AegisState> riskNode(RiskAgent agent,
            AgentOutputValidator validator,
            NodeTracer tracer) {
        return state -> {
            String input = """
                    User query: %s
                    Execution plan: %s
                    Research findings: %s
                    Evaluate risks for the above context.
                    """.formatted(state.userQuery(), state.plannerOutput(), state.researchOutput());

            Instant start = Instant.now();
            try {
                String output = agent.call(input);
                tracer.trace(RISK, input, output, start);
                assertValid(validator.validateRisk(output), output);
                return Map.of(AegisState.RISK_OUTPUT, output);
            } catch (Exception e) {
                tracer.traceError(RISK, input, start, e);
                throw e;
            }
        };
    }

    private NodeAction<AegisState> criticNode(CriticAgent agent,
            AgentOutputValidator validator,
            NodeTracer tracer) {
        return state -> {
            String input = """
                    User query: %s
                    Plan: %s
                    Research: %s
                    Risk assessment: %s
                    Review all of the above for quality, completeness, and consistency.
                    """.formatted(
                    state.userQuery(),
                    state.plannerOutput(),
                    state.researchOutput(),
                    state.riskOutput());

            Instant start = Instant.now();
            try {
                String output = agent.call(input);
                tracer.trace(CRITIC, input, output, start);
                assertValid(validator.validateCritic(output), output);

                String verdict = extractVerdict(output);
                int newRevisionCount = state.revisionCount() + 1;

                return Map.of(
                        AegisState.CRITIC_OUTPUT, output,
                        AegisState.CRITIC_VERDICT, verdict,
                        AegisState.REVISION_COUNT, newRevisionCount);
            } catch (Exception e) {
                tracer.traceError(CRITIC, input, start, e);
                throw e;
            }
        };
    }

    private NodeAction<AegisState> synthesizerNode(SynthesizerAgent agent,
            AgentOutputValidator validator,
            NodeTracer tracer) {
        return state -> {
            String input = """
                    User query: %s
                    Plan: %s
                    Research: %s
                    Risk assessment: %s
                    Critic review: %s
                    Synthesise a final, actionable response from all the above.
                    """.formatted(
                    state.userQuery(),
                    state.plannerOutput(),
                    state.researchOutput(),
                    state.riskOutput(),
                    state.criticOutput());

            Instant start = Instant.now();
            try {
                String output = agent.call(input);
                tracer.trace(SYNTHESIZER, input, output, start);
                assertValid(validator.validateSynthesizer(output), output);
                // No delay needed for the last node
                return Map.of(AegisState.SYNTHESIZER_OUTPUT, output);
            } catch (Exception e) {
                tracer.traceError(SYNTHESIZER, input, start, e);
                throw e;
            }
        };
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private void assertValid(AgentOutputValidator.ValidationResult result, String rawOutput) {
        if (!result.valid()) {
            log.error("Validation failed: {} | Raw output: {}", result.errorSummary(),
                    rawOutput.length() > 300 ? rawOutput.substring(0, 300) + "…" : rawOutput);
            throw new AgentValidationException(result);
        }
    }

    private String extractVerdict(String criticJson) {
        try {
            JsonNode node = objectMapper.readTree(criticJson);
            if (node.has("verdict")) {
                return node.get("verdict").asText().toUpperCase();
            }
        } catch (Exception e) {
            log.warn("Failed to parse critic JSON for verdict — defaulting to APPROVE", e);
        }
        return "APPROVE";
    }
}
