package com.safetylaw.monitor.dto;

/**
 * 설정 화면에 내려보내는 값.
 *
 * <p>SMTP 비밀번호는 포함하지 않는다. 저장은 되지만 화면으로 다시 내보내지 않는다.
 */
public record SettingsResponse(
        boolean demoMode,
        boolean lawApiOcSet,
        String lawApiOc,
        int autoSyncIntervalHours,
        boolean smtpConfigured,
        String smtpHost,
        Integer smtpPort,
        Boolean smtpUseTls,
        String smtpUser,
        String smtpFrom,
        String alertEmails,
        String newAdmrulKeywords,
        String newAdmrulDepartment,
        String newAdmrulSinceDate,
        boolean fullLawCacheEnabled,
        boolean emailFeatureEnabled,
        boolean newsTickerEnabled,
        String newsSourceMoelUrl,
        String newsSourceKoshaUrl,
        String newsSourceAccidentUrl,
        int newsMaxItemsPerCategory) {
}
