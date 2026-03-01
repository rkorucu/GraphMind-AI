package com.aegis.backend.agent;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;

/**
 * Shared state object that flows through the Aegis agent graph.
 *
 * <p>
 * Every node reads from and writes to this state. Keys use overwrite
 * semantics (via {@link Channels#base}) — each graph iteration replaces
 * the previous value.
 */
public class AegisState extends AgentState {

    // ── State Keys ──────────────────────────────────────────────────────
    public static final String USER_QUERY = "userQuery";
    public static final String PLANNER_OUTPUT = "plannerOutput";
    public static final String RESEARCH_OUTPUT = "researchOutput";
    public static final String RISK_OUTPUT = "riskOutput";
    public static final String CRITIC_OUTPUT = "criticOutput";
    public static final String CRITIC_VERDICT = "criticVerdict";
    public static final String SYNTHESIZER_OUTPUT = "synthesizerOutput";
    public static final String REVISION_COUNT = "revisionCount";

    // ── Schema ──────────────────────────────────────────────────────────
    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            USER_QUERY, Channels.base(() -> ""),
            PLANNER_OUTPUT, Channels.base(() -> ""),
            RESEARCH_OUTPUT, Channels.base(() -> ""),
            RISK_OUTPUT, Channels.base(() -> ""),
            CRITIC_OUTPUT, Channels.base(() -> ""),
            CRITIC_VERDICT, Channels.base(() -> ""),
            SYNTHESIZER_OUTPUT, Channels.base(() -> ""),
            REVISION_COUNT, Channels.base(() -> 0));

    public AegisState(Map<String, Object> initData) {
        super(initData);
    }

    // ── Typed Accessors ─────────────────────────────────────────────────

    public String userQuery() {
        return this.<String>value(USER_QUERY).orElse("");
    }

    public String plannerOutput() {
        return this.<String>value(PLANNER_OUTPUT).orElse("");
    }

    public String researchOutput() {
        return this.<String>value(RESEARCH_OUTPUT).orElse("");
    }

    public String riskOutput() {
        return this.<String>value(RISK_OUTPUT).orElse("");
    }

    public String criticOutput() {
        return this.<String>value(CRITIC_OUTPUT).orElse("");
    }

    public String criticVerdict() {
        return this.<String>value(CRITIC_VERDICT).orElse("");
    }

    public String synthesizerOutput() {
        return this.<String>value(SYNTHESIZER_OUTPUT).orElse("");
    }

    public int revisionCount() {
        return this.<Integer>value(REVISION_COUNT).orElse(0);
    }
}
