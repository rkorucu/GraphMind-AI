package com.aegis.backend.agent;

import com.aegis.backend.agent.AgentOutputValidator.ValidationResult;

/**
 * Thrown when an agent's JSON output fails schema validation.
 */
public class AgentValidationException extends RuntimeException {

    private final ValidationResult validationResult;

    public AgentValidationException(ValidationResult validationResult) {
        super(validationResult.errorSummary());
        this.validationResult = validationResult;
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }
}
