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
 * Deterministic financial projection tool.
 *
 * <p>
 * Computes 1-year, 3-year, and 5-year projections based on sector
 * growth rates, compounding logic, and risk-adjusted returns. All logic
 * is pure Java — no external calls.
 *
 * <p>
 * Example output:
 * 
 * <pre>{@code
 * {
 *   "sector": "Renewable Energy",
 *   "baseGrowthRate": 12.5,
 *   "projections": [
 *     { "horizon": "1Y", "projectedReturn": 12.5, "riskAdjustedReturn": 10.0, "confidence": "HIGH" },
 *     { "horizon": "3Y", "projectedReturn": 42.6, "riskAdjustedReturn": 33.1, "confidence": "MEDIUM" },
 *     { "horizon": "5Y", "projectedReturn": 81.4, "riskAdjustedReturn": 60.2, "confidence": "LOW" }
 *   ],
 *   "assumptions": ["Consistent sector growth", "No major regulatory changes", "Stable macroeconomic environment"]
 * }
 * }</pre>
 */
@Component
public class FinancialProjectionTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Base growth rates by sector keyword
    private static final Map<String, Double> SECTOR_GROWTH = Map.ofEntries(
            Map.entry("renewable", 12.5),
            Map.entry("solar", 12.5),
            Map.entry("wind", 12.5),
            Map.entry("clean energy", 12.5),
            Map.entry("technology", 9.8),
            Map.entry("tech", 9.8),
            Map.entry("ai", 15.0),
            Map.entry("software", 9.8),
            Map.entry("healthcare", 5.2),
            Map.entry("pharma", 5.2),
            Map.entry("biotech", 8.0),
            Map.entry("finance", 4.1),
            Map.entry("banking", 4.1),
            Map.entry("fintech", 7.5),
            Map.entry("real estate", 2.1),
            Map.entry("crypto", 18.0),
            Map.entry("bitcoin", 18.0));

    // Risk discount factors by horizon
    private static final double RISK_DISCOUNT_1Y = 0.80;
    private static final double RISK_DISCOUNT_3Y = 0.70;
    private static final double RISK_DISCOUNT_5Y = 0.60;

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Projects financial returns for the sector identified in the query.
     *
     * @param query natural-language query
     * @return JSON string with projections at 1Y, 3Y, and 5Y horizons
     */
    public String project(String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        String matchedSector = "General Market";
        double baseRate = 3.5; // default

        for (Map.Entry<String, Double> entry : SECTOR_GROWTH.entrySet()) {
            if (lower.contains(entry.getKey())) {
                matchedSector = capitalise(entry.getKey());
                baseRate = entry.getValue();
                break;
            }
        }

        return toJson(matchedSector, baseRate);
    }

    // ── Internal ────────────────────────────────────────────────────────

    private String toJson(String sector, double baseRate) {
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("sector", sector);
            root.put("baseGrowthRate", baseRate);

            ArrayNode projections = root.putArray("projections");

            addProjection(projections, "1Y", baseRate, 1, RISK_DISCOUNT_1Y, "HIGH");
            addProjection(projections, "3Y", baseRate, 3, RISK_DISCOUNT_3Y, "MEDIUM");
            addProjection(projections, "5Y", baseRate, 5, RISK_DISCOUNT_5Y, "LOW");

            ArrayNode assumptions = root.putArray("assumptions");
            assumptions.add("Consistent sector growth at historical averages");
            assumptions.add("No major regulatory or policy disruptions");
            assumptions.add("Stable macroeconomic environment");
            assumptions.add("Compounding returns reinvested");

            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"error\": \"Failed to compute projections\"}";
        }
    }

    private void addProjection(ArrayNode array, String horizon, double annualRate,
            int years, double riskDiscount, String confidence) {
        // Compound growth: (1 + r)^n - 1
        double compounded = (Math.pow(1 + annualRate / 100.0, years) - 1) * 100.0;
        double riskAdjusted = compounded * riskDiscount;

        ObjectNode p = MAPPER.createObjectNode();
        p.put("horizon", horizon);
        p.put("projectedReturn", round(compounded));
        p.put("riskAdjustedReturn", round(riskAdjusted));
        p.put("confidence", confidence);
        array.add(p);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private String capitalise(String s) {
        if (s == null || s.isEmpty())
            return s;
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
