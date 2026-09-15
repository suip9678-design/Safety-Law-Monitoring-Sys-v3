package com.safetylaw.monitor.dto;

/**
 * 설정 변경 요청.
 *
 * <p>값이 null 인 항목은 "바꾸지 않음"을 뜻한다. 빈 문자열은 "비워서 저장"이라
 * 뜻이 다르므로 구분해서 다룬다.
 */
public record SettingsUpdateRequest(
        String lawApiOc,
        String smtpHost,
        Integer smtpPort,
        Boolean smtpUseTls,
        String smtpUser,
        String smtpPassword,
        String smtpFrom,
        String alertEmails,
        String newAdmrulKeywords,
        String newAdmrulDepartment,
        String newAdmrulSinceDate,
        Boolean fullLawCacheEnabled,
        Boolean newsTickerEnabled,
        String newsSourceMoelUrl,
        String newsSourceKoshaUrl,
        String newsSourceAccidentUrl,
        Integer newsMaxItemsPerCategory) {
}
