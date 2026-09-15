package com.safetylaw.monitor.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.safetylaw.monitor.config.AppProperties;
import com.safetylaw.monitor.domain.NewAdmrulCandidate;
import com.safetylaw.monitor.dto.SyncResultResponse;
import com.safetylaw.monitor.lawapi.LawApiClient;
import com.safetylaw.monitor.lawapi.LawApiClientFactory;
import com.safetylaw.monitor.mapper.TrackedLawMapper;

/**
 * 화면의 "지금 확인" 버튼이 하는 일.
 *
 * <p>개정 확인, 본문 갱신, 신규 제정 고시 탐지, 알림 메일을 차례로 수행한다.
 * 뒤쪽 단계가 실패해도 앞 단계의 결과는 살아 있어야 하므로, 실패는 예외로
 * 올리지 않고 결과의 오류 목록에 담아 화면에 보여준다.
 */
@Service
public class SyncFacade {

    private final TrackedLawMapper trackedLawMapper;
    private final LawApiClientFactory clientFactory;
    private final SettingsService settings;
    private final SyncService syncService;
    private final ContentCacheService contentCacheService;
    private final EmailService emailService;
    private final AppProperties properties;

    public SyncFacade(TrackedLawMapper trackedLawMapper,
                      LawApiClientFactory clientFactory,
                      SettingsService settings,
                      SyncService syncService,
                      ContentCacheService contentCacheService,
                      EmailService emailService,
                      AppProperties properties) {
        this.trackedLawMapper = trackedLawMapper;
        this.clientFactory = clientFactory;
        this.settings = settings;
        this.syncService = syncService;
        this.contentCacheService = contentCacheService;
        this.emailService = emailService;
        this.properties = properties;
    }

    public SyncResultResponse run() {
        LawApiClient client = clientFactory.create(settings.get(SettingsService.LAW_API_OC));

        int checked = trackedLawMapper.countActive();
        SyncService.SyncOutcome outcome = syncService.syncAll(client);
        List<String> errors = new ArrayList<>(outcome.errors());

        try {
            contentCacheService.refreshTrackedLawContent(client);
        } catch (RuntimeException e) {
            errors.add("본문 캐시 갱신 실패: " + e.getMessage());
        }

        int newCandidates = 0;
        try {
            List<String> keywords = splitCsv(settings.get(SettingsService.NEW_ADMRUL_KEYWORDS));
            String department = settings.get(SettingsService.NEW_ADMRUL_DEPARTMENT).trim();
            String sinceDate = settings.get(SettingsService.NEW_ADMRUL_SINCE_DATE).trim();
            List<NewAdmrulCandidate> found =
                    syncService.scanNewAdmrul(client, keywords, department, sinceDate);
            newCandidates = found.size();
        } catch (RuntimeException e) {
            errors.add("신규 고시 탐색 실패: " + e.getMessage());
        }

        if (!outcome.newRevisions().isEmpty() && properties.email().enabled()) {
            try {
                emailService.sendRevisionAlert(outcome.newRevisions());
            } catch (RuntimeException e) {
                errors.add("이메일 발송 실패: " + e.getMessage());
            }
        }

        return new SyncResultResponse(checked, outcome.newRevisions().size(), newCandidates, errors);
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
