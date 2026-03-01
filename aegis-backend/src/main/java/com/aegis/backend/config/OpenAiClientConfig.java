package com.aegis.backend.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * OpenAI client configuration.
 *
 * <p>
 * The {@link ChatLanguageModel} bean is auto-configured by the
 * {@code langchain4j-open-ai-spring-boot-starter} based on values in
 * {@code application.yml} (under {@code langchain4j.open-ai.chat-model}).
 *
 * <p>
 * This class serves as the central place to add any extra wiring or
 * customisation on top of that auto-configured bean.
 */
@Configuration
public class OpenAiClientConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClientConfig.class);

    private final ChatLanguageModel chatLanguageModel;

    public OpenAiClientConfig(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    @PostConstruct
    void init() {
        log.info("OpenAI ChatLanguageModel bean initialised: {}", chatLanguageModel.getClass().getSimpleName());
    }
}
