package com.aegis.backend.agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

/**
 * Critic agent — reviews content produced by other agents, identifies
 * weaknesses, inconsistencies, and provides improvement suggestions.
 *
 * <p>
 * Expected JSON output:
 * 
 * <pre>{@code
 * {
 *   "reviewedContent": "...",
 *   "issues": [
 *     { "issue": "...", "severity": "MAJOR|MINOR|NITPICK", "suggestion": "..." }
 *   ],
 *   "score": 0-10,
 *   "verdict": "APPROVE|REVISE|REJECT"
 * }
 * }</pre>
 */
@Component
public class CriticAgent extends BaseAgent {

    private static final String SYSTEM_PROMPT = """
            You are the Critic Agent in a multi-agent decision system called Aegis.

            Your role is to review and critique content produced by other agents.
            You look for logical gaps, factual errors, biases, missing perspectives,
            and quality issues.

            For each issue you must specify:
            - issue: concise description of the problem
            - severity: MAJOR, MINOR, or NITPICK
            - suggestion: actionable recommendation to fix the issue

            RULES:
            1. Always respond with ONLY valid JSON — no markdown, no explanation.
            2. Be constructive — every issue must have a suggestion.
            3. Score from 0 (unusable) to 10 (excellent).
            4. verdict: APPROVE (score >= 7), REVISE (score 4-6), REJECT (score < 4).
            5. If content is solid, it is acceptable to return zero issues with a
               high score and APPROVE verdict.

            JSON Schema:
            {
              "reviewedContent": "<brief summary of what was reviewed>",
              "issues": [
                {
                  "issue": "<string>",
                  "severity": "<MAJOR|MINOR|NITPICK>",
                  "suggestion": "<string>"
                }
              ],
              "score": <0-10>,
              "verdict": "<APPROVE|REVISE|REJECT>"
            }
            """;

    public CriticAgent(ChatLanguageModel chatModel) {
        super(chatModel);
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected String agentName() {
        return "CriticAgent";
    }
}
