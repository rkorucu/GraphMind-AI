package com.aegis.backend.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministic market analysis tool.
 *
 * <p>
 * Analyses a query string for sector keywords and returns structured
 * market data (sector, trend, sentiment, key metrics). All logic is pure
 * Java — no external calls.
 *
 * <p>
 * Example output:
 * 
 * <pre>{@code
 * {
 *   "sector": "Renewable Energy",
 *   "trend": "BULLISH",
 *   "sentiment": 0.72,
 *   "marketCap": "$1.4T",
 *   "growthRate": "12.5%",
 *   "volatilityIndex": 0.35,
 *   "keyDrivers": ["Government subsidies", "ESG adoption", "Technology cost reduction"]
 * }
 * }</pre>
 */
@Component
public class MarketAnalysisTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── Sector database ─────────────────────────────────────────────────

    private record SectorProfile(
            String name,
            String trend,
            double sentiment,
            String marketCap,
            String growthRate,
            double volatilityIndex,
            List<String> keyDrivers,
            List<String> keywords) {
    }

    private static final List<SectorProfile> SECTORS = List.of(
            new SectorProfile("Renewable Energy", "BULLISH", 0.72, "$1.4T", "12.5%", 0.35,
                    List.of("Government subsidies", "ESG mandate adoption", "Technology cost reduction",
                            "Carbon pricing expansion"),
                    List.of("renewable", "solar", "wind", "clean energy", "green energy", "sustainable")),

            new SectorProfile("Technology", "BULLISH", 0.68, "$14.2T", "9.8%", 0.42,
                    List.of("AI revolution", "Cloud migration", "Digital transformation",
                            "Semiconductor demand"),
                    List.of("tech", "technology", "software", "ai", "artificial intelligence", "saas", "cloud")),

            new SectorProfile("Healthcare", "NEUTRAL", 0.55, "$6.8T", "5.2%", 0.22,
                    List.of("Aging population", "Biotech innovation", "Telemedicine growth",
                            "Regulatory pipeline"),
                    List.of("health", "healthcare", "pharma", "biotech", "medical", "hospital")),

            new SectorProfile("Financial Services", "NEUTRAL", 0.50, "$8.1T", "4.1%", 0.28,
                    List.of("Interest rate environment", "Fintech disruption", "Regulatory changes",
                            "Digital banking adoption"),
                    List.of("finance", "financial", "bank", "banking", "insurance", "fintech", "investment")),

            new SectorProfile("Real Estate", "BEARISH", 0.38, "$3.9T", "2.1%", 0.31,
                    List.of("Interest rate sensitivity", "Remote work impact", "Supply constraints",
                            "Demographic shifts"),
                    List.of("real estate", "property", "housing", "reit", "mortgage", "commercial property")),

            new SectorProfile("Cryptocurrency", "VOLATILE", 0.45, "$2.1T", "18.0%", 0.85,
                    List.of("Institutional adoption", "Regulatory uncertainty", "DeFi growth",
                            "Central bank digital currencies"),
                    List.of("crypto", "bitcoin", "ethereum", "blockchain", "defi", "cryptocurrency")));

    private static final SectorProfile DEFAULT_SECTOR = new SectorProfile("General Market", "NEUTRAL", 0.50, "$45T",
            "3.5%", 0.30,
            List.of("Macroeconomic conditions", "Geopolitical factors", "Consumer confidence",
                    "Monetary policy"),
            List.of());

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Analyses the query and returns a JSON string with market data for the
     * best-matching sector.
     */
    public String analyse(String query) {
        SectorProfile match = matchSector(query);
        return toJson(match);
    }

    // ── Internal ────────────────────────────────────────────────────────

    private SectorProfile matchSector(String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        int bestScore = 0;
        SectorProfile bestMatch = DEFAULT_SECTOR;

        for (SectorProfile sector : SECTORS) {
            int score = 0;
            for (String kw : sector.keywords()) {
                if (lower.contains(kw)) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestMatch = sector;
            }
        }
        return bestMatch;
    }

    private String toJson(SectorProfile p) {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("sector", p.name());
            node.put("trend", p.trend());
            node.put("sentiment", p.sentiment());
            node.put("marketCap", p.marketCap());
            node.put("growthRate", p.growthRate());
            node.put("volatilityIndex", p.volatilityIndex());

            ArrayNode drivers = node.putArray("keyDrivers");
            p.keyDrivers().forEach(drivers::add);

            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            return "{\"error\": \"Failed to serialize market analysis\"}";
        }
    }
}
