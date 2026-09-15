package com.safetylaw.monitor.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * application.yml 의 app.* 설정.
 *
 * <p>여기 값은 "기본값"이고, 사용자가 설정 화면에서 바꾼 값은 DB 의
 * APP_SETTINGS 테이블에 저장되어 이 값보다 우선한다
 * (SettingsService 참고).
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(

        /** 국가법령정보 공동활용 API 인증키(OC). 비어 있으면 데모 모드. */
        @DefaultValue("") String lawApiOc,

        @DefaultValue("https://www.law.go.kr") String lawApiBaseUrl,

        /** 자동 동기화 주기(시간). 0 이면 동작하지 않음. */
        @DefaultValue("24") int autoSyncIntervalHours,

        @DefaultValue("0 0 1 * * *") String dailyMaintenanceCron,

        @DefaultValue("Asia/Seoul") String timezone,

        @DefaultValue NewAdmrul newAdmrul,

        @DefaultValue("false") boolean fullLawCacheEnabled,

        @DefaultValue Dashboard dashboard,

        @DefaultValue Email email,

        @DefaultValue News news
) {

    /** API 인증키가 없으면 실제 조회 대신 예시 데이터로 동작한다. */
    public boolean demoMode() {
        return lawApiOc == null || lawApiOc.isBlank();
    }

    public record NewAdmrul(
            @DefaultValue("안전보건,산업안전,중대재해,위험성평가,유해위험,보건관리,안전관리")
            String keywords,
            @DefaultValue("고용노동부") String department,
            /** YYYYMMDD. 이 날짜 이전 공포분은 제외. 비우면 전체 대상. */
            @DefaultValue("") String sinceDate
    ) {
        public List<String> keywordList() {
            return splitCsv(keywords);
        }
    }

    public record Dashboard(
            @DefaultValue("") String username,
            @DefaultValue("") String password
    ) {
        /** 아이디와 비밀번호가 모두 설정된 경우에만 로그인을 요구한다. */
        public boolean loginRequired() {
            return username != null && !username.isBlank()
                    && password != null && !password.isBlank();
        }
    }

    public record Email(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("") String from,
            @DefaultValue("") String alertRecipients
    ) {
        public List<String> recipientList() {
            return splitCsv(alertRecipients);
        }
    }

    public record News(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("3") int fetchIntervalHours,
            @DefaultValue("30") int maxItemsPerCategory,
            @DefaultValue Sources sources
    ) {
    }

    public record Sources(
            @DefaultValue("") String moel,
            @DefaultValue("") String kosha,
            @DefaultValue("") String accident
    ) {
    }

    private static List<String> splitCsv(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }
}
