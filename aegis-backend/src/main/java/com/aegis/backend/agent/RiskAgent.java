package com.aegis.backend.agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

/**
 * Risk agent — evaluates potential risks, threats, and downsides
 * associated with a given decision or context.
 *
 * <p>
 * Expected JSON output:
 * 
 * <pre>{@code
 * {
 *   "context": "...",
 *   "risks": [
 *     { "risk": "...", "severity": "CRITICAL|HIGH|MEDIUM|LOW", "likelihood": "...", "mitigation": "..." }
 *   ],
 *   "overallRiskLevel": "CRITICAL|HIGH|MEDIUM|LOW",
 *   "assessment": "..."
 * }
 * }</pre>
 */
@Component
public class RiskAgent extends BaseAgent {

    private static final String SYSTEM_PROMPT = """
            You are the Risk Agent in a multi-agent decision system called Aegis.

            Your role is to identify and evaluate risks associated with a given
            decision, plan, or context.

            For each risk you must specify:
            - risk: concise description of the risk
            - severity: CRITICAL, HIGH, MEDIUM, or LOW
            - likelihood: estimated probability description
            - mitigation: recommended action to reduce this risk

            RULES:
            1. Always respond with ONLY valid JSON — no markdown, no explanation.
            2. Provide at least 1 risk and at most 8.
            3. overallRiskLevel should reflect the aggregate assessment.
            4. Be thorough but avoid hypothetical edge-cases with negligible impact.

            JSON Schema:
            {
              "context": "<restated context>",
              "risks": [
                {
                  "risk": "<string>",
                  "severity": "<CRITICAL|HIGH|MEDIUM|LOW>",
                  "likelihood": "<string>",
                  "mitigation": "<string>"
                }
              ],
              "overallRiskLevel": "<CRITICAL|HIGH|MEDIUM|LOW>",
              "assessment": "<1-3 sentence overall risk summary>"
            }
            """;

    public RiskAgent(ChatLanguageModel chatModel) {
        super(chatModel);
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected String agentName() {
        return "RiskAgent";
    }
}
