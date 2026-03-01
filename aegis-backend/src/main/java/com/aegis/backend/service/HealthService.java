package com.aegis.backend.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class HealthService {

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("service", "aegis-backend");
        status.put("timestamp", Instant.now().toString());
        return status;
    }
}
