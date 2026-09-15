package com.safetylaw.monitor.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.safetylaw.monitor.dto.FullCacheStatus;
import com.safetylaw.monitor.dto.KeywordSearchResult;
import com.safetylaw.monitor.lawapi.LawApiClient;
import com.safetylaw.monitor.lawapi.LawApiClientFactory;
import com.safetylaw.monitor.service.ContentCacheService;
import com.safetylaw.monitor.service.ScheduledJobs;
import com.safetylaw.monitor.service.SettingsService;

@RestController
@RequestMapping("/api/content-cache")
public class ContentCacheController {

    private final ContentCacheService contentCacheService;
    private final ScheduledJobs scheduledJobs;
    private final LawApiClientFactory clientFactory;
    private final SettingsService settings;

    public ContentCacheController(ContentCacheService contentCacheService,
                                  ScheduledJobs scheduledJobs,
                                  LawApiClientFactory clientFactory,
                                  SettingsService settings) {
        this.contentCacheService = contentCacheService;
        this.scheduledJobs = scheduledJobs;
        this.clientFactory = clientFactory;
        this.settings = settings;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return contentCacheService.cacheStatus();
    }

    /**
     * 전체 법령 본문 캐시를 시작한다.
     *
     * <p>수천~수만 건을 순회할 수 있어 요청 안에서 끝내지 않고 따로 돌린다.
     * 화면은 진행 상황 조회를 주기적으로 불러 상태를 표시한다.
     */
    @PostMapping("/full-refresh")
    public Map<String, Boolean> startFullRefresh() {
        if (!scheduledJobs.startFullCacheInBackground()) {
            throw new ConflictException("이미 전체 법령 캐시가 진행 중입니다.");
        }
        return Map.of("started", true);
    }

    @GetMapping("/full-refresh-status")
    public FullCacheStatus fullRefreshStatus() {
        return contentCacheService.fullCacheStatus();
    }

    @GetMapping("/search")
    public List<KeywordSearchResult> search(
            @RequestParam("query") String query,
            @RequestParam(name = "source_type", defaultValue = "all") String sourceType) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("검색어를 입력하세요.");
        }
        if (!List.of("all", "law", "admrul").contains(sourceType)) {
            throw new IllegalArgumentException("source_type은 all, law, admrul 중 하나여야 합니다.");
        }
        return new ArrayList<>(contentCacheService.searchCache(sourceType, query));
    }

    /** 추적 중인 법령에 더해, 키워드로 찾아지는 범위까지 본문을 채운다. */
    @PostMapping("/refresh")
    public Map<String, Object> refresh() {
        LawApiClient client = clientFactory.create(settings.get(SettingsService.LAW_API_OC));
        List<String> keywords = splitCsv(settings.get(SettingsService.NEW_ADMRUL_KEYWORDS));
        String department = settings.get(SettingsService.NEW_ADMRUL_DEPARTMENT).trim();

        int refreshed = contentCacheService.refreshCandidateContent(client, keywords, department);

        Map<String, Object> body = new LinkedHashMap<>(contentCacheService.cacheStatus());
        body.put("refreshed", refreshed);
        return body;
    }

    private static List<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
