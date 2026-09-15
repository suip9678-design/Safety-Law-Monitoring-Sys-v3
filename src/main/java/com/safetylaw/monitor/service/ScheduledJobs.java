package com.safetylaw.monitor.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import com.safetylaw.monitor.config.AppProperties;
import com.safetylaw.monitor.domain.NewAdmrulCandidate;
import com.safetylaw.monitor.lawapi.LawApiClient;
import com.safetylaw.monitor.lawapi.LawApiClientFactory;
import com.safetylaw.monitor.support.Times;

/**
 * 정기 실행 작업.
 *
 * <p>상시 켜 두는 서버가 아니라 필요할 때 켜서 쓰는 사용 방식을 전제로 한다.
 * 그래서 "매일 새벽 1시" 작업은 그 시각에 컴퓨터가 꺼져 있으면 그냥 지나가므로,
 * 서버가 켜질 때마다 오늘 몫이 돌았는지 확인해 안 돌았으면 대신 한 번 실행한다.
 */
@Component
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    /**
     * 시작 직후 한 번 채우기까지 두는 간격.
     *
     * <p>뉴스는 서버를 켤 때마다 잠시 뒤 한 번 바로 채워, 처음 띄웠을 때
     * 게시판이 몇 시간 동안 비어 보이지 않게 한다. 유지보수 따라잡기는
     * 다른 시동 작업과 겹치지 않도록 조금 뒤에 돈다.
     */
    private static final Duration NEWS_INITIAL_DELAY = Duration.ofSeconds(20);
    private static final Duration MAINTENANCE_CATCHUP_DELAY = Duration.ofSeconds(15);

    /**
     * 이 시간 안에 유지보수가 돌았으면 오늘 몫은 이미 처리된 것으로 본다.
     *
     * <p>매일 켜는 시각이 들쭉날쭉해도 하루씩 밀리지 않도록 24시간보다 짧게 잡는다.
     */
    private static final Duration MAINTENANCE_FRESH_WINDOW = Duration.ofHours(20);

    private final ThreadPoolTaskScheduler scheduler;
    private final ExecutorService fullCacheExecutor;
    private final AppProperties properties;
    private final SettingsService settings;
    private final LawApiClientFactory clientFactory;
    private final SyncService syncService;
    private final ContentCacheService contentCacheService;
    private final NewsService newsService;
    private final EmailService emailService;

    public ScheduledJobs(ThreadPoolTaskScheduler scheduler,
                         ExecutorService fullCacheExecutor,
                         AppProperties properties,
                         SettingsService settings,
                         LawApiClientFactory clientFactory,
                         SyncService syncService,
                         ContentCacheService contentCacheService,
                         NewsService newsService,
                         EmailService emailService) {
        this.scheduler = scheduler;
        this.fullCacheExecutor = fullCacheExecutor;
        this.properties = properties;
        this.settings = settings;
        this.clientFactory = clientFactory;
        this.syncService = syncService;
        this.contentCacheService = contentCacheService;
        this.newsService = newsService;
        this.emailService = emailService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerJobs() {
        int syncHours = properties.autoSyncIntervalHours();
        if (syncHours > 0) {
            scheduler.scheduleAtFixedRate(this::runSync,
                    Instant.now().plus(Duration.ofHours(syncHours)), Duration.ofHours(syncHours));
            log.info("자동 동기화 예약 ({}시간 주기)", syncHours);
        }

        int newsHours = properties.news().fetchIntervalHours();
        if (newsHours > 0) {
            scheduler.scheduleAtFixedRate(this::runNewsSync,
                    Instant.now().plus(NEWS_INITIAL_DELAY), Duration.ofHours(newsHours));
            log.info("안전보건 뉴스 수집 예약 ({}시간 주기)", newsHours);
        }

        scheduler.schedule(this::runDailyMaintenance,
                new CronTrigger(properties.dailyMaintenanceCron(), Times.KST));
        log.info("일일 유지보수 예약 (cron: {}, {})", properties.dailyMaintenanceCron(), Times.KST);

        scheduleMaintenanceCatchUpIfNeeded();
    }

    /**
     * 새벽 작업을 놓쳤다면 시작 직후 한 번 대신 실행한다.
     *
     * <p>컴퓨터가 꺼져 있던 동안의 몫을 따라잡기 위한 것이다.
     */
    private void scheduleMaintenanceCatchUpIfNeeded() {
        LocalDateTime lastAt = lastMaintenanceAt();
        boolean stale = lastAt == null
                || Duration.between(lastAt, Times.nowUtc()).compareTo(MAINTENANCE_FRESH_WINDOW) >= 0;
        if (!stale) {
            return;
        }
        scheduler.schedule(this::runDailyMaintenance, Instant.now().plus(MAINTENANCE_CATCHUP_DELAY));
        log.info("최근 {}시간 안에 일일 유지보수 기록이 없어 시작 직후 한 번 대신 실행합니다.",
                MAINTENANCE_FRESH_WINDOW.toHours());
    }

    /** 등록된 법령의 개정 확인 + 본문 갱신 + 알림 메일. */
    public void runSync() {
        try {
            LawApiClient client = clientFactory.create(settings.get(SettingsService.LAW_API_OC));
            SyncService.SyncOutcome outcome = syncService.syncAll(client);
            contentCacheService.refreshTrackedLawContent(client);

            if (!outcome.newRevisions().isEmpty() && properties.email().enabled()) {
                try {
                    emailService.sendRevisionAlert(outcome.newRevisions());
                } catch (RuntimeException e) {
                    log.warn("개정 알림 메일을 보내지 못했습니다: {}", e.getMessage());
                }
            }
            if (!outcome.errors().isEmpty()) {
                log.warn("자동 동기화 중 오류: {}", outcome.errors());
            }
            log.info("자동 동기화 완료: 신규 개정 {}건", outcome.newRevisions().size());
        } catch (RuntimeException e) {
            log.error("자동 동기화 중 오류", e);
        }
    }

    /** 뉴스 게시판 채우기. 실패해도 다른 작업을 막지 않는다. */
    public void runNewsSync() {
        try {
            if (!settings.getBoolean(SettingsService.NEWS_TICKER_ENABLED)) {
                return;
            }
            int maxItems = settings.getInt(SettingsService.NEWS_MAX_ITEMS_PER_CATEGORY,
                    properties.news().maxItemsPerCategory());
            int added = newsService.syncNews(newsService.configuredSources(), maxItems);
            log.info("안전보건 뉴스 수집 완료: 신규 {}건", added);
        } catch (RuntimeException e) {
            log.error("안전보건 뉴스 수집 중 오류", e);
        }
    }

    /**
     * 매일 한 번 도는 작업 모음.
     *
     * <ul>
     *   <li>신규 제정 고시 탐지 - 하루 한 번이면 충분한 가벼운 작업이라
     *       사용자가 직접 새로고침하지 않아도 항상 돈다.</li>
     *   <li>전체 법령 본문 캐시 - 상세 조회를 수천 건 호출하는 무거운
     *       작업이라 설정에서 켠 경우에만 돈다.</li>
     * </ul>
     */
    public void runDailyMaintenance() {
        try {
            LawApiClient client = clientFactory.create(settings.get(SettingsService.LAW_API_OC));

            try {
                List<String> keywords = splitCsv(settings.get(SettingsService.NEW_ADMRUL_KEYWORDS));
                String department = settings.get(SettingsService.NEW_ADMRUL_DEPARTMENT).trim();
                String sinceDate = settings.get(SettingsService.NEW_ADMRUL_SINCE_DATE).trim();
                List<NewAdmrulCandidate> found =
                        syncService.scanNewAdmrul(client, keywords, department, sinceDate);
                log.info("신규 제정 고시 탐지 완료: {}건", found.size());
            } catch (RuntimeException e) {
                // 이 작업이 실패해도 아래 본문 캐시까지 막지는 않는다.
                log.error("신규 제정 고시 탐지 중 오류", e);
            }

            if (settings.getBoolean(SettingsService.FULL_LAW_CACHE_ENABLED)) {
                int count = contentCacheService.refreshFullLawContent(
                        client, List.of(LawApiClient.LAW, LawApiClient.ADMRUL));
                log.info("전체 법령 자동 캐시 완료: {}건", count);
            }
        } catch (RuntimeException e) {
            log.error("일일 유지보수 중 오류", e);
        } finally {
            settings.setInternal(SettingsService.LAST_DAILY_MAINTENANCE_AT, Times.nowUtc().toString());
        }
    }

    /** 화면에서 전체 법령 캐시를 시작시킬 때 쓴다. 이미 돌고 있으면 시작하지 않는다. */
    public boolean startFullCacheInBackground() {
        if (contentCacheService.isFullCacheRunning()) {
            return false;
        }
        fullCacheExecutor.submit(() -> {
            LawApiClient client = clientFactory.create(settings.get(SettingsService.LAW_API_OC));
            contentCacheService.refreshFullLawContent(client,
                    List.of(LawApiClient.LAW, LawApiClient.ADMRUL));
        });
        return true;
    }

    private LocalDateTime lastMaintenanceAt() {
        String raw = settings.getInternal(SettingsService.LAST_DAILY_MAINTENANCE_AT);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw.trim());
        } catch (RuntimeException e) {
            return null;
        }
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
