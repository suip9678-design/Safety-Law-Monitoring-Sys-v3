package com.safetylaw.monitor.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.safetylaw.monitor.config.AppProperties;
import com.safetylaw.monitor.dto.SettingsResponse;
import com.safetylaw.monitor.dto.SettingsUpdateRequest;

/** 설정 화면이 읽고 쓰는 값의 형태를 맞춘다. */
@Service
public class SettingsFacade {

    private final SettingsService settings;
    private final AppProperties properties;

    public SettingsFacade(SettingsService settings, AppProperties properties) {
        this.settings = settings;
        this.properties = properties;
    }

    public SettingsResponse current() {
        Map<String, String> values = settings.getAll();
        String lawApiOc = values.getOrDefault(SettingsService.LAW_API_OC, "");
        String smtpHost = values.getOrDefault(SettingsService.SMTP_HOST, "");
        String alertEmails = values.getOrDefault(SettingsService.ALERT_EMAILS, "");

        return new SettingsResponse(
                lawApiOc.isBlank(),
                !lawApiOc.isBlank(),
                blankToNull(lawApiOc),
                properties.autoSyncIntervalHours(),
                !smtpHost.isBlank() && !alertEmails.isBlank(),
                blankToNull(smtpHost),
                parseInt(values.get(SettingsService.SMTP_PORT), 587),
                SettingsService.isTruthy(values.getOrDefault(SettingsService.SMTP_USE_TLS, "true")),
                blankToNull(values.getOrDefault(SettingsService.SMTP_USER, "")),
                blankToNull(values.getOrDefault(SettingsService.SMTP_FROM, "")),
                blankToNull(alertEmails),
                blankToNull(values.getOrDefault(SettingsService.NEW_ADMRUL_KEYWORDS, "")),
                blankToNull(values.getOrDefault(SettingsService.NEW_ADMRUL_DEPARTMENT, "")),
                blankToNull(values.getOrDefault(SettingsService.NEW_ADMRUL_SINCE_DATE, "")),
                SettingsService.isTruthy(values.getOrDefault(SettingsService.FULL_LAW_CACHE_ENABLED, "false")),
                properties.email().enabled(),
                SettingsService.isTruthy(values.getOrDefault(SettingsService.NEWS_TICKER_ENABLED, "true")),
                blankToNull(values.getOrDefault(SettingsService.NEWS_SOURCE_MOEL_URL, "")),
                blankToNull(values.getOrDefault(SettingsService.NEWS_SOURCE_KOSHA_URL, "")),
                blankToNull(values.getOrDefault(SettingsService.NEWS_SOURCE_ACCIDENT_URL, "")),
                parseInt(values.get(SettingsService.NEWS_MAX_ITEMS_PER_CATEGORY), 30));
    }

    /** 값이 null 인 항목은 건드리지 않는다. 빈 문자열은 "비워서 저장"이라 뜻이 다르다. */
    public SettingsResponse update(SettingsUpdateRequest request) {
        Map<String, String> updates = new LinkedHashMap<>();
        put(updates, SettingsService.LAW_API_OC, request.lawApiOc());
        put(updates, SettingsService.SMTP_HOST, request.smtpHost());
        put(updates, SettingsService.SMTP_PORT, request.smtpPort());
        put(updates, SettingsService.SMTP_USE_TLS, request.smtpUseTls());
        put(updates, SettingsService.SMTP_USER, request.smtpUser());
        put(updates, SettingsService.SMTP_PASSWORD, request.smtpPassword());
        put(updates, SettingsService.SMTP_FROM, request.smtpFrom());
        put(updates, SettingsService.ALERT_EMAILS, request.alertEmails());
        put(updates, SettingsService.NEW_ADMRUL_KEYWORDS, request.newAdmrulKeywords());
        put(updates, SettingsService.NEW_ADMRUL_DEPARTMENT, request.newAdmrulDepartment());
        put(updates, SettingsService.NEW_ADMRUL_SINCE_DATE, request.newAdmrulSinceDate());
        put(updates, SettingsService.FULL_LAW_CACHE_ENABLED, request.fullLawCacheEnabled());
        put(updates, SettingsService.NEWS_TICKER_ENABLED, request.newsTickerEnabled());
        put(updates, SettingsService.NEWS_SOURCE_MOEL_URL, request.newsSourceMoelUrl());
        put(updates, SettingsService.NEWS_SOURCE_KOSHA_URL, request.newsSourceKoshaUrl());
        put(updates, SettingsService.NEWS_SOURCE_ACCIDENT_URL, request.newsSourceAccidentUrl());
        put(updates, SettingsService.NEWS_MAX_ITEMS_PER_CATEGORY, request.newsMaxItemsPerCategory());

        settings.setValues(updates);
        return current();
    }

    public boolean helpShown() {
        return "1".equals(settings.getInternal(SettingsService.HELP_SHOWN));
    }

    public void markHelpShown() {
        settings.setInternal(SettingsService.HELP_SHOWN, "1");
    }

    private static void put(Map<String, String> updates, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Boolean flag) {
            updates.put(key, flag ? "true" : "false");
        } else {
            updates.put(key, String.valueOf(value));
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return (raw == null || raw.isBlank()) ? fallback : Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
