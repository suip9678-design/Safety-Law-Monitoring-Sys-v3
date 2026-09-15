package com.safetylaw.monitor.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safetylaw.monitor.config.AppProperties;
import com.safetylaw.monitor.domain.AppSetting;
import com.safetylaw.monitor.mapper.AppSettingMapper;

/**
 * 실행 중에 바꿀 수 있는 설정.
 *
 * <p>설정 화면에서 API 인증키나 메일 설정을 바꾸면 서버를 다시 띄우지 않아도
 * 반영된다. 값은 APP_SETTINGS 테이블에 저장되고, 저장된 적 없는 항목만
 * application.yml 의 기본값을 쓴다.
 */
@Service
public class SettingsService {

    public static final String LAW_API_OC = "law_api_oc";
    public static final String SMTP_HOST = "smtp_host";
    public static final String SMTP_PORT = "smtp_port";
    public static final String SMTP_USE_TLS = "smtp_use_tls";
    public static final String SMTP_USER = "smtp_user";
    public static final String SMTP_PASSWORD = "smtp_password";
    public static final String SMTP_FROM = "smtp_from";
    public static final String ALERT_EMAILS = "alert_emails";
    public static final String NEW_ADMRUL_KEYWORDS = "new_admrul_keywords";
    public static final String NEW_ADMRUL_DEPARTMENT = "new_admrul_department";
    public static final String NEW_ADMRUL_SINCE_DATE = "new_admrul_since_date";
    public static final String FULL_LAW_CACHE_ENABLED = "full_law_cache_enabled";
    public static final String NEWS_TICKER_ENABLED = "news_ticker_enabled";
    public static final String NEWS_SOURCE_MOEL_URL = "news_source_moel_url";
    public static final String NEWS_SOURCE_KOSHA_URL = "news_source_kosha_url";
    public static final String NEWS_SOURCE_ACCIDENT_URL = "news_source_accident_url";
    public static final String NEWS_MAX_ITEMS_PER_CATEGORY = "news_max_items_per_category";

    /** 설정 화면에서 바꿀 수 있는 항목. 여기 없는 키는 저장하지 않는다. */
    private static final List<String> KEYS = List.of(
            LAW_API_OC, SMTP_HOST, SMTP_PORT, SMTP_USE_TLS, SMTP_USER, SMTP_PASSWORD,
            SMTP_FROM, ALERT_EMAILS, NEW_ADMRUL_KEYWORDS, NEW_ADMRUL_DEPARTMENT,
            NEW_ADMRUL_SINCE_DATE, FULL_LAW_CACHE_ENABLED, NEWS_TICKER_ENABLED,
            NEWS_SOURCE_MOEL_URL, NEWS_SOURCE_KOSHA_URL, NEWS_SOURCE_ACCIDENT_URL,
            NEWS_MAX_ITEMS_PER_CATEGORY);

    /** 이 이름들은 별도 저장 항목이 아니라 유지보수 기록용이라 KEYS 에 넣지 않는다. */
    public static final String LAST_DAILY_MAINTENANCE_AT = "last_daily_maintenance_at";
    public static final String HELP_SHOWN = "help_shown";

    private final AppSettingMapper mapper;
    private final AppProperties properties;

    public SettingsService(AppSettingMapper mapper, AppProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
    }

    /**
     * 현재 유효한 설정 전체.
     *
     * <p>어떤 항목을 사용자가 "빈 값으로" 저장한 것과 한 번도 저장하지 않은
     * 것은 구분해야 한다. 소관부처 칸을 비워 저장하는 것은 "필터를 걸지
     * 않겠다"는 뜻인데, 이를 구분하지 않으면 저장할 때마다 기본값이 다시
     * 채워진다.
     *
     * <p>Oracle 은 빈 문자열을 NULL 로 저장하므로 값이 NULL 인지로는 구분할 수
     * 없다. 그래서 <b>행이 있는지</b>로 저장 여부를 판단하고, 값이 NULL 이면
     * 빈 문자열로 본다.
     */
    public Map<String, String> getAll() {
        Map<String, String> stored = new HashMap<>();
        for (AppSetting row : mapper.findAll()) {
            stored.put(row.getSettingKey(), row.getSettingValue());
        }

        Map<String, String> result = new LinkedHashMap<>(defaults());
        for (String key : KEYS) {
            if (stored.containsKey(key)) {
                String value = stored.get(key);
                result.put(key, value == null ? "" : value);
            }
        }
        return result;
    }

    public String get(String key) {
        return getAll().getOrDefault(key, "");
    }

    public boolean getBoolean(String key) {
        return isTruthy(get(key));
    }

    public int getInt(String key, int fallback) {
        String raw = get(key);
        try {
            return raw == null || raw.isBlank() ? fallback : Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** 설정 화면에서 바꿀 수 있는 항목만 저장한다. 값이 null 인 항목은 건드리지 않는다. */
    @Transactional
    public void setValues(Map<String, String> values) {
        values.forEach((key, value) -> {
            if (KEYS.contains(key) && value != null) {
                mapper.upsert(key, value);
            }
        });
    }

    /** 설정 화면 항목이 아닌 내부 기록용 값. */
    public String getInternal(String key) {
        AppSetting row = mapper.findByKey(key);
        return row == null ? null : row.getSettingValue();
    }

    @Transactional
    public void setInternal(String key, String value) {
        mapper.upsert(key, value);
    }

    public static boolean isTruthy(String value) {
        if (value == null) {
            return false;
        }
        return switch (value.trim().toLowerCase()) {
            case "1", "true", "yes", "on" -> true;
            default -> false;
        };
    }

    private Map<String, String> defaults() {
        AppProperties.Email email = properties.email();
        AppProperties.NewAdmrul admrul = properties.newAdmrul();
        AppProperties.News news = properties.news();

        Map<String, String> map = new LinkedHashMap<>();
        map.put(LAW_API_OC, properties.lawApiOc());
        map.put(SMTP_HOST, email.host());
        map.put(SMTP_PORT, String.valueOf(email.port()));
        map.put(SMTP_USE_TLS, String.valueOf(email.useTls()));
        map.put(SMTP_USER, email.username());
        map.put(SMTP_PASSWORD, email.password());
        map.put(SMTP_FROM, email.from());
        map.put(ALERT_EMAILS, email.alertRecipients());
        map.put(NEW_ADMRUL_KEYWORDS, admrul.keywords());
        map.put(NEW_ADMRUL_DEPARTMENT, admrul.department());
        map.put(NEW_ADMRUL_SINCE_DATE, admrul.sinceDate());
        map.put(FULL_LAW_CACHE_ENABLED, String.valueOf(properties.fullLawCacheEnabled()));
        map.put(NEWS_TICKER_ENABLED, String.valueOf(news.enabled()));
        map.put(NEWS_SOURCE_MOEL_URL, news.sources().moel());
        map.put(NEWS_SOURCE_KOSHA_URL, news.sources().kosha());
        map.put(NEWS_SOURCE_ACCIDENT_URL, news.sources().accident());
        map.put(NEWS_MAX_ITEMS_PER_CATEGORY, String.valueOf(news.maxItemsPerCategory()));
        return map;
    }
}
