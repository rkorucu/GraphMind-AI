package com.aegis.backend.agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

/**
 * Planner agent — decomposes a high-level objective into a structured,
 * ordered execution plan with defined steps and reasoning.
 *
 * <p>
 * Expected JSON output:
 * 
 * <pre>{@code
 * {
 *   "objective": "...",
 *   "steps": [
 *     { "stepNumber": 1, "action": "...", "assignedAgent": "...", "expectedOutput": "..." }
 *   ],
 *   "reasoning": "..."
 * }
 * }</pre>
 */
@Component
public class PlannerAgent extends BaseAgent {

    private static final String SYSTEM_PROMPT = """
            You are the Planner Agent in a multi-agent decision system called Aegis.

            Your role is to take a high-level user objective and decompose it into a
            clear, ordered execution plan.

            For each step you must specify:
            - stepNumber: sequential integer
            - action: concise description of what needs to be done
            - assignedAgent: which downstream agent should handle this step
              (Research, Risk, Critic, or Synthesizer)
            - expectedOutput: what the step should produce

            RULES:
            1. Always respond with ONLY valid JSON — no markdown, no explanation.
            2. The plan must be logically ordered with dependencies respected.
            3. Keep the plan concise: prefer 3-7 steps.

            JSON Schema:
            {
              "objective": "<restated objective>",
              "steps": [
                {
                  "stepNumber": <int>,
                  "action": "<string>",
                  "assignedAgent": "<Research|Risk|Critic|Synthesizer>",
                  "expectedOutput": "<string>"
                }
              ],
              "reasoning": "<brief rationale for this plan>"
            }
            """;

    public PlannerAgent(ChatLanguageModel chatModel) {
        super(chatModel);
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected String agentName() {
        return "PlannerAgent";
    }
}
