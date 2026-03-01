package com.aegis.backend.agent;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Base class for all Aegis agent personas.
 *
 * <p>
 * Subclasses define a {@link #systemPrompt()} and optionally override
 * behaviour. The shared {@link #call(String)} method sends the system +
 * user messages to the {@link ChatLanguageModel} and returns the raw
 * model response as a {@code String} (expected to be JSON).
 */
public abstract class BaseAgent {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final ChatLanguageModel chatModel;

    protected BaseAgent(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    /** The persona-specific system prompt. */
    protected abstract String systemPrompt();

    /** Human-readable agent name (used in logs). */
    protected abstract String agentName();

    /**
     * Sends a user query to the model with the agent's system prompt and
     * returns the raw response text (expected to be valid JSON).
     */
    public String call(String userQuery) {
        log.info("[{}] Processing query: {}", agentName(),
                userQuery.length() > 120 ? userQuery.substring(0, 120) + "…" : userQuery);

        List<ChatMessage> messages = List.of(
                SystemMessage.from(systemPrompt()),
                UserMessage.from(userQuery));

        ChatResponse response = chatModel.chat(messages);
        String output = response.aiMessage().text();

        log.debug("[{}] Response received ({} chars)", agentName(), output.length());
        return output;
    }
}
