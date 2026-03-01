package com.aegis.backend.agent;

import com.aegis.backend.tools.FinancialProjectionTool;
import com.aegis.backend.tools.MarketAnalysisTool;
import com.aegis.backend.tools.RiskScoringTool;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

/**
 * Research agent — gathers, analyses, and summarises information
 * relevant to a given query or topic.
 *
 * <p>
 * Enriches the LLM call with deterministic tool outputs from
 * {@link MarketAnalysisTool}, {@link FinancialProjectionTool}, and
 * {@link RiskScoringTool}, giving the model concrete data to reason over.
 *
 * <p>
 * Expected JSON output:
 * 
 * <pre>{@code
 * {
 *   "query": "...",
 *   "findings": [
 *     { "title": "...", "detail": "...", "confidence": "HIGH|MEDIUM|LOW" }
 *   ],
 *   "summary": "..."
 * }
 * }</pre>
 */
@Component
public class ResearchAgent extends BaseAgent {

    private final MarketAnalysisTool marketAnalysisTool;
    private final FinancialProjectionTool financialProjectionTool;
    private final RiskScoringTool riskScoringTool;

    private static final String SYSTEM_PROMPT = """
            You are the Research Agent in a multi-agent decision system called Aegis.

            Your role is to thoroughly research a given query and return structured
            findings backed by analysis and reasoning.

            You will be provided with TOOL DATA from three deterministic analysis tools:
            1. Market Analysis — sector trends, sentiment, key drivers
            2. Financial Projections — 1Y / 3Y / 5Y return projections
            3. Risk Scoring — multi-dimensional risk assessment

            Use the tool data as the factual foundation for your findings. You may add
            your own analytical insights on top, but do NOT contradict the tool data.

            For each finding you must specify:
            - title: a short label for the finding
            - detail: supporting explanation or evidence
            - confidence: HIGH, MEDIUM, or LOW

            RULES:
            1. Always respond with ONLY valid JSON — no markdown, no explanation.
            2. Provide at least 3 findings and at most 10.
            3. At least one finding must reference market analysis tool data.
            4. At least one finding must reference financial projections.
            5. At least one finding must reference risk scoring.
            6. The summary should synthesise the key takeaway in 1-3 sentences.
            7. Be objective and evidence-driven.

            JSON Schema:
            {
              "query": "<restated query>",
              "findings": [
                {
                  "title": "<string>",
                  "detail": "<string>",
                  "confidence": "<HIGH|MEDIUM|LOW>"
                }
              ],
              "summary": "<string>"
            }
            """;

    public ResearchAgent(ChatLanguageModel chatModel,
            MarketAnalysisTool marketAnalysisTool,
            FinancialProjectionTool financialProjectionTool,
            RiskScoringTool riskScoringTool) {
        super(chatModel);
        this.marketAnalysisTool = marketAnalysisTool;
        this.financialProjectionTool = financialProjectionTool;
        this.riskScoringTool = riskScoringTool;
    }

    /**
     * Overrides the base {@code call()} to enrich the user query with
     * deterministic tool data before sending to the LLM.
     */
    @Override
    public String call(String userQuery) {
        log.info("[ResearchAgent] Running deterministic tools …");

        String marketData = marketAnalysisTool.analyse(userQuery);
        String projectionData = financialProjectionTool.project(userQuery);
        String riskData = riskScoringTool.score(userQuery);

        log.debug("[ResearchAgent] Market data:     {}", marketData);
        log.debug("[ResearchAgent] Projection data: {}", projectionData);
        log.debug("[ResearchAgent] Risk data:       {}", riskData);

        String enrichedQuery = """
                USER QUERY:
                %s

                === TOOL DATA (use as factual basis) ===

                [Market Analysis]
                %s

                [Financial Projections]
                %s

                [Risk Scoring]
                %s
                """.formatted(userQuery, marketData, projectionData, riskData);

        return super.call(enrichedQuery);
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected String agentName() {
        return "ResearchAgent";
    }
}
