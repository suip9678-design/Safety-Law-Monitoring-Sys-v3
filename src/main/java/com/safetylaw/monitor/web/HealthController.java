package com.safetylaw.monitor.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.safetylaw.monitor.service.SettingsService;

/** 로그인 없이 열려 있는 상태 점검용 경로. */
@RestController
public class HealthController {

    private final SettingsService settings;

    public HealthController(SettingsService settings) {
        this.settings = settings;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        boolean demoMode = settings.get(SettingsService.LAW_API_OC).isBlank();
        return Map.of("status", "ok", "demo_mode", demoMode);
    }
}
