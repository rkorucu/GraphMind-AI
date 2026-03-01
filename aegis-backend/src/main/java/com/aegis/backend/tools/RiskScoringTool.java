package com.aegis.backend.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministic risk scoring tool.
 *
 * <p>
 * Evaluates investment risk across multiple dimensions (market, regulatory,
 * liquidity, concentration, operational) and computes a weighted composite
 * score. All logic is pure Java — no external calls.
 *
 * <p>
 * Example output:
 * 
 * <pre>{@code
 * {
 *   "sector": "Renewable Energy",
 *   "compositeRiskScore": 5.4,
 *   "riskLevel": "MEDIUM",
 *   "dimensions": [
 *     { "dimension": "Market Risk",     "score": 6.0, "weight": 0.30 },
 *     { "dimension": "Regulatory Risk", "score": 5.0, "weight": 0.25 },
 *     ...
 *   ],
 *   "recommendation": "Moderate exposure recommended with diversification"
 * }
 * }</pre>
 */
@Component
public class RiskScoringTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── Risk profiles ───────────────────────────────────────────────────

    private record RiskDimension(String name, double score, double weight) {
    }

    private record RiskProfile(
            String sector,
            List<RiskDimension> dimensions,
            List<String> keywords) {
    }

    private static final List<RiskProfile> PROFILES = List.of(
            new RiskProfile("Renewable Energy", List.of(
                    new RiskDimension("Market Risk", 6.0, 0.30),
                    new RiskDimension("Regulatory Risk", 5.0, 0.25),
                    new RiskDimension("Liquidity Risk", 3.5, 0.15),
                    new RiskDimension("Concentration Risk", 7.0, 0.15),
                    new RiskDimension("Operational Risk", 4.5, 0.15)),
                    List.of("renewable", "solar", "wind", "clean energy", "green")),

            new RiskProfile("Technology", List.of(
                    new RiskDimension("Market Risk", 7.0, 0.30),
                    new RiskDimension("Regulatory Risk", 4.0, 0.20),
                    new RiskDimension("Liquidity Risk", 2.5, 0.15),
                    new RiskDimension("Concentration Risk", 6.5, 0.20),
                    new RiskDimension("Operational Risk", 5.0, 0.15)),
                    List.of("tech", "technology", "software", "ai", "cloud")),

            new RiskProfile("Healthcare", List.of(
                    new RiskDimension("Market Risk", 4.0, 0.25),
                    new RiskDimension("Regulatory Risk", 7.5, 0.30),
                    new RiskDimension("Liquidity Risk", 3.0, 0.15),
                    new RiskDimension("Concentration Risk", 5.0, 0.15),
                    new RiskDimension("Operational Risk", 4.0, 0.15)),
                    List.of("health", "healthcare", "pharma", "biotech", "medical")),

            new RiskProfile("Financial Services", List.of(
                    new RiskDimension("Market Risk", 5.5, 0.25),
                    new RiskDimension("Regulatory Risk", 6.5, 0.30),
                    new RiskDimension("Liquidity Risk", 3.0, 0.15),
                    new RiskDimension("Concentration Risk", 4.5, 0.15),
                    new RiskDimension("Operational Risk", 5.5, 0.15)),
                    List.of("finance", "financial", "bank", "banking", "fintech")),

            new RiskProfile("Cryptocurrency", List.of(
                    new RiskDimension("Market Risk", 9.0, 0.30),
                    new RiskDimension("Regulatory Risk", 8.5, 0.25),
                    new RiskDimension("Liquidity Risk", 5.0, 0.15),
                    new RiskDimension("Concentration Risk", 7.0, 0.15),
                    new RiskDimension("Operational Risk", 7.5, 0.15)),
                    List.of("crypto", "bitcoin", "ethereum", "blockchain", "defi")));

    private static final RiskProfile DEFAULT_PROFILE = new RiskProfile(
            "General Market", List.of(
                    new RiskDimension("Market Risk", 5.0, 0.30),
                    new RiskDimension("Regulatory Risk", 4.0, 0.20),
                    new RiskDimension("Liquidity Risk", 3.0, 0.20),
                    new RiskDimension("Concentration Risk", 4.0, 0.15),
                    new RiskDimension("Operational Risk", 3.5, 0.15)),
            List.of());

    // ── Risk level thresholds ───────────────────────────────────────────

    private static final Map<String, String> RECOMMENDATIONS = Map.of(
            "LOW", "Suitable for conservative portfolios; standard position sizing",
            "MEDIUM", "Moderate exposure recommended with diversification",
            "HIGH", "Limit exposure; consider hedging strategies",
            "CRITICAL", "Extreme caution; speculative allocation only");

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Scores risk for the sector identified in the query.
     *
     * @param query natural-language query
     * @return JSON string with dimensional risk scores and composite rating
     */
    public String score(String query) {
        RiskProfile profile = matchProfile(query);
        double composite = computeComposite(profile);
        String level = classifyRisk(composite);
        return toJson(profile, composite, level);
    }

    // ── Internal ────────────────────────────────────────────────────────

    private RiskProfile matchProfile(String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        int bestScore = 0;
        RiskProfile best = DEFAULT_PROFILE;

        for (RiskProfile p : PROFILES) {
            int score = 0;
            for (String kw : p.keywords()) {
                if (lower.contains(kw))
                    score++;
            }
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        return best;
    }

    private double computeComposite(RiskProfile profile) {
        return profile.dimensions().stream()
                .mapToDouble(d -> d.score() * d.weight())
                .sum();
    }

    private String classifyRisk(double composite) {
        if (composite <= 3.0)
            return "LOW";
        if (composite <= 5.5)
            return "MEDIUM";
        if (composite <= 7.5)
            return "HIGH";
        return "CRITICAL";
    }

    private String toJson(RiskProfile profile, double composite, String level) {
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("sector", profile.sector());
            root.put("compositeRiskScore", round(composite));
            root.put("riskLevel", level);

            ArrayNode dims = root.putArray("dimensions");
            for (RiskDimension d : profile.dimensions()) {
                ObjectNode dim = MAPPER.createObjectNode();
                dim.put("dimension", d.name());
                dim.put("score", d.score());
                dim.put("weight", d.weight());
                dims.add(dim);
            }

            root.put("recommendation", RECOMMENDATIONS.getOrDefault(level, "Review required"));
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"error\": \"Failed to compute risk score\"}";
        }
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
