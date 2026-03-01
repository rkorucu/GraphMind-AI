package com.aegis.backend.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates JSON output from each Aegis agent against its expected schema.
 *
 * <p>
 * Each agent has a dedicated validation method that checks for required
 * fields, correct types, and valid enum values. Validation is pure Jackson
 * parsing — no external schema library needed.
 */
@Component
public class AgentOutputValidator {

    private static final Logger log = LoggerFactory.getLogger(AgentOutputValidator.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── Valid enum sets ─────────────────────────────────────────────────
    private static final Set<String> CONFIDENCE_LEVELS = Set.of("HIGH", "MEDIUM", "LOW");
    private static final Set<String> RISK_SEVERITIES = Set.of("CRITICAL", "HIGH", "MEDIUM", "LOW");
    private static final Set<String> ASSIGNED_AGENTS = Set.of("RESEARCH", "RISK", "CRITIC", "SYNTHESIZER");
    private static final Set<String> ISSUE_SEVERITIES = Set.of("MAJOR", "MINOR", "NITPICK");
    private static final Set<String> VERDICTS = Set.of("APPROVE", "REVISE", "REJECT");

    // ── Public validation methods ───────────────────────────────────────

    /**
     * Validates Planner output.
     * Required: {@code objective} (string), {@code steps} (array of objects),
     * {@code reasoning} (string).
     */
    public ValidationResult validatePlanner(String json) {
        return validate("PlannerAgent", json, root -> {
            List<String> errors = new ArrayList<>();
            requireString(root, "objective", errors);
            requireString(root, "reasoning", errors);

            if (!root.has("steps") || !root.get("steps").isArray()) {
                errors.add("Missing or invalid 'steps' array");
            } else {
                for (int i = 0; i < root.get("steps").size(); i++) {
                    JsonNode step = root.get("steps").get(i);
                    String prefix = "steps[" + i + "]";
                    requireField(step, "stepNumber", prefix, errors);
                    requireString(step, "action", prefix, errors);
                    requireEnum(step, "assignedAgent", ASSIGNED_AGENTS, prefix, errors);
                    requireString(step, "expectedOutput", prefix, errors);
                }
            }
            return errors;
        });
    }

    /**
     * Validates Research output.
     * Required: {@code query} (string), {@code findings} (array),
     * {@code summary} (string).
     */
    public ValidationResult validateResearch(String json) {
        return validate("ResearchAgent", json, root -> {
            List<String> errors = new ArrayList<>();
            requireString(root, "query", errors);
            requireString(root, "summary", errors);

            if (!root.has("findings") || !root.get("findings").isArray()) {
                errors.add("Missing or invalid 'findings' array");
            } else {
                for (int i = 0; i < root.get("findings").size(); i++) {
                    JsonNode finding = root.get("findings").get(i);
                    String prefix = "findings[" + i + "]";
                    requireString(finding, "title", prefix, errors);
                    requireString(finding, "detail", prefix, errors);
                    requireEnum(finding, "confidence", CONFIDENCE_LEVELS, prefix, errors);
                }
            }
            return errors;
        });
    }

    /**
     * Validates Risk output.
     * Required: {@code context} (string), {@code risks} (array),
     * {@code overallRiskLevel} (enum), {@code assessment} (string).
     */
    public ValidationResult validateRisk(String json) {
        return validate("RiskAgent", json, root -> {
            List<String> errors = new ArrayList<>();
            requireString(root, "context", errors);
            requireString(root, "assessment", errors);
            requireEnum(root, "overallRiskLevel", RISK_SEVERITIES, errors);

            if (!root.has("risks") || !root.get("risks").isArray()) {
                errors.add("Missing or invalid 'risks' array");
            } else {
                for (int i = 0; i < root.get("risks").size(); i++) {
                    JsonNode risk = root.get("risks").get(i);
                    String prefix = "risks[" + i + "]";
                    requireString(risk, "risk", prefix, errors);
                    requireEnum(risk, "severity", RISK_SEVERITIES, prefix, errors);
                    requireString(risk, "likelihood", prefix, errors);
                    requireString(risk, "mitigation", prefix, errors);
                }
            }
            return errors;
        });
    }

    /**
     * Validates Critic output.
     * Required: {@code reviewedContent} (string), {@code issues} (array),
     * {@code score} (number 0-10), {@code verdict} (enum).
     */
    public ValidationResult validateCritic(String json) {
        return validate("CriticAgent", json, root -> {
            List<String> errors = new ArrayList<>();
            requireString(root, "reviewedContent", errors);
            requireEnum(root, "verdict", VERDICTS, errors);

            if (!root.has("score") || !root.get("score").isNumber()) {
                errors.add("Missing or invalid 'score' (expected number)");
            } else {
                int score = root.get("score").asInt();
                if (score < 0 || score > 10) {
                    errors.add("'score' must be between 0 and 10, got: " + score);
                }
            }

            if (!root.has("issues") || !root.get("issues").isArray()) {
                errors.add("Missing or invalid 'issues' array");
            } else {
                for (int i = 0; i < root.get("issues").size(); i++) {
                    JsonNode issue = root.get("issues").get(i);
                    String prefix = "issues[" + i + "]";
                    requireString(issue, "issue", prefix, errors);
                    requireEnum(issue, "severity", ISSUE_SEVERITIES, prefix, errors);
                    requireString(issue, "suggestion", prefix, errors);
                }
            }
            return errors;
        });
    }

    /**
     * Validates Synthesizer output.
     * Required: {@code inputSummary} (string), {@code synthesis} (string),
     * {@code keyPoints} (array of strings), {@code recommendation} (string),
     * {@code confidenceLevel} (enum).
     */
    public ValidationResult validateSynthesizer(String json) {
        return validate("SynthesizerAgent", json, root -> {
            List<String> errors = new ArrayList<>();
            requireString(root, "inputSummary", errors);
            requireString(root, "synthesis", errors);
            requireString(root, "recommendation", errors);
            requireEnum(root, "confidenceLevel", CONFIDENCE_LEVELS, errors);

            if (!root.has("keyPoints") || !root.get("keyPoints").isArray()) {
                errors.add("Missing or invalid 'keyPoints' array");
            } else {
                for (int i = 0; i < root.get("keyPoints").size(); i++) {
                    if (!root.get("keyPoints").get(i).isTextual()) {
                        errors.add("keyPoints[" + i + "] must be a string");
                    }
                }
            }
            return errors;
        });
    }

    // ── Result type ─────────────────────────────────────────────────────

    public record ValidationResult(boolean valid, String agentName, List<String> errors) {

        public String errorSummary() {
            if (valid)
                return "OK";
            return "[%s] Validation failed: %s".formatted(agentName, String.join("; ", errors));
        }
    }

    // ── Internal helpers ────────────────────────────────────────────────

    @FunctionalInterface
    private interface SchemaValidator {
        List<String> validate(JsonNode root);
    }

    private String cleanJson(String raw) {
        if (raw == null)
            return null;
        String cleaned = raw.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private ValidationResult validate(String agentName, String json, SchemaValidator schema) {
        try {
            String cleanedJson = cleanJson(json);
            JsonNode root = MAPPER.readTree(cleanedJson);
            if (root == null || root.isMissingNode()) {
                return new ValidationResult(false, agentName, List.of("Empty or null JSON"));
            }
            List<String> errors = schema.validate(root);
            if (!errors.isEmpty()) {
                log.warn("[{}] Schema validation failed: {}", agentName, errors);
            }
            return new ValidationResult(errors.isEmpty(), agentName, errors);
        } catch (Exception e) {
            log.error("[{}] Failed to parse JSON output", agentName, e);
            return new ValidationResult(false, agentName,
                    List.of("Invalid JSON: " + e.getMessage()));
        }
    }

    private void requireString(JsonNode node, String field, List<String> errors) {
        requireString(node, field, null, errors);
    }

    private void requireString(JsonNode node, String field, String prefix, List<String> errors) {
        String path = prefix != null ? prefix + "." + field : field;
        if (!node.has(field) || !node.get(field).isTextual() || node.get(field).asText().isBlank()) {
            errors.add("Missing or empty '" + path + "'");
        }
    }

    private void requireField(JsonNode node, String field, String prefix, List<String> errors) {
        String path = prefix != null ? prefix + "." + field : field;
        if (!node.has(field) || node.get(field).isNull()) {
            errors.add("Missing '" + path + "'");
        }
    }

    private void requireEnum(JsonNode node, String field, Set<String> allowed, List<String> errors) {
        requireEnum(node, field, allowed, null, errors);
    }

    private void requireEnum(JsonNode node, String field, Set<String> allowed,
            String prefix, List<String> errors) {
        String path = prefix != null ? prefix + "." + field : field;
        if (!node.has(field) || !node.get(field).isTextual()) {
            errors.add("Missing or non-string '" + path + "'");
        } else {
            String value = node.get(field).asText().toUpperCase();
            if (!allowed.contains(value)) {
                errors.add("Invalid '" + path + "': '" + value + "' not in " + allowed);
            }
        }
    }
}
