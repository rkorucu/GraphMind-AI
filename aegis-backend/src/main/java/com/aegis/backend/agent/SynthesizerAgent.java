package com.aegis.backend.agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

/**
 * Synthesizer agent — combines outputs from multiple agents into a
 * coherent, actionable final response.
 *
 * <p>
 * Expected JSON output:
 * 
 * <pre>{@code
 * {
 *   "inputSummary": "...",
 *   "synthesis": "...",
 *   "keyPoints": ["...", "..."],
 *   "recommendation": "...",
 *   "confidenceLevel": "HIGH|MEDIUM|LOW"
 * }
 * }</pre>
 */
@Component
public class SynthesizerAgent extends BaseAgent {

    private static final String SYSTEM_PROMPT = """
            You are the Synthesizer Agent in a multi-agent decision system called Aegis.

            Your role is to combine outputs from multiple upstream agents (Research,
            Risk, Critic, etc.) into a single, coherent, and actionable final response.

            RULES:
            1. Always respond with ONLY valid JSON — no markdown, no explanation.
            2. synthesis: a well-structured narrative combining all inputs.
            3. keyPoints: the 3-5 most important takeaways, ordered by importance.
            4. recommendation: a clear, actionable next step or decision.
            5. confidenceLevel: HIGH, MEDIUM, or LOW — based on the quality and
               agreement of the upstream inputs.
            6. Do NOT fabricate information that was not present in the inputs.

            JSON Schema:
            {
              "inputSummary": "<brief summary of what was received>",
              "synthesis": "<string>",
              "keyPoints": ["<string>", "..."],
              "recommendation": "<string>",
              "confidenceLevel": "<HIGH|MEDIUM|LOW>"
            }
            """;

    public SynthesizerAgent(ChatLanguageModel chatModel) {
        super(chatModel);
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected String agentName() {
        return "SynthesizerAgent";
    }
}
