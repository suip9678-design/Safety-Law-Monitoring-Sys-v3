package com.safetylaw.monitor.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safetylaw.monitor.domain.LawRevision;
import com.safetylaw.monitor.domain.NewAdmrulCandidate;
import com.safetylaw.monitor.domain.TrackedLaw;
import com.safetylaw.monitor.lawapi.LawApiClient;
import com.safetylaw.monitor.lawapi.LawApiException;
import com.safetylaw.monitor.lawapi.LawApiItem;
import com.safetylaw.monitor.mapper.LawRevisionMapper;
import com.safetylaw.monitor.mapper.NewAdmrulCandidateMapper;
import com.safetylaw.monitor.mapper.TrackedLawMapper;
import com.safetylaw.monitor.support.Times;

/**
 * 등록된 법령·고시의 개정 확인과, 아직 등록하지 않은 신규 제정 고시 탐지.
 */
@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    public static final String STATUS_UNREVIEWED = "미검토";
    public static final String STATUS_IN_REVIEW = "검토중";
    public static final String STATUS_REFLECTED = "반영완료";
    public static final String STATUS_NOT_APPLICABLE = "해당없음";

    public static final List<String> REVIEW_STATUSES =
            List.of(STATUS_UNREVIEWED, STATUS_IN_REVIEW, STATUS_REFLECTED, STATUS_NOT_APPLICABLE);

    public static final String CANDIDATE_NEW = "신규";
    public static final String CANDIDATE_REGISTERED = "등록됨";
    public static final String CANDIDATE_DISMISSED = "무시됨";

    /**
     * 신규 제정 고시 탐지에서 키워드 하나당 훑을 목록 조회의 크기와 최대 페이지.
     *
     * <p>100건 x 2000페이지 = 키워드당 최대 20만 건이다. 현실적으로 이 한도에
     * 걸리지 않지만, 걸리는 경우 조용히 잘리지 않도록 경고를 남긴다.
     */
    private static final int SCAN_PAGE_SIZE = 100;
    private static final int SCAN_MAX_PAGES = 2000;

    private final TrackedLawMapper trackedLawMapper;
    private final LawRevisionMapper revisionMapper;
    private final NewAdmrulCandidateMapper candidateMapper;
    private final ObjectMapper objectMapper;

    public SyncService(TrackedLawMapper trackedLawMapper,
                       LawRevisionMapper revisionMapper,
                       NewAdmrulCandidateMapper candidateMapper,
                       ObjectMapper objectMapper) {
        this.trackedLawMapper = trackedLawMapper;
        this.revisionMapper = revisionMapper;
        this.candidateMapper = candidateMapper;
        this.objectMapper = objectMapper;
    }

    /** 동기화 결과. */
    public record SyncOutcome(int checked, List<LawRevision> newRevisions, List<String> errors) {
    }

    /** 활성 상태인 법령·고시 전체를 확인한다. */
    public SyncOutcome syncAll(LawApiClient client) {
        List<TrackedLaw> laws = trackedLawMapper.findActive();
        List<LawRevision> newRevisions = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (TrackedLaw law : laws) {
            try {
                LawRevision revision = syncOne(law, client);
                if (revision != null) {
                    newRevisions.add(revision);
                }
            } catch (LawApiException e) {
                errors.add(law.getName() + ": " + e.getMessage());
            }
        }
        return new SyncOutcome(laws.size(), newRevisions, errors);
    }

    /**
     * 법령 하나를 이름으로 다시 검색해, 공포/시행 정보가 지난번 확인 이후
     * 달라졌으면 개정 이력을 남긴다.
     *
     * <p>처음 확인하는 경우에도 "미검토" 이력을 하나 남긴다. 추적 대상으로
     * 등록하는 것 자체가 한 번은 살펴봐야 할 일이기 때문이다. 이 경우
     * 직전 값(previous*)이 비어 있어 실제 개정과 구분된다.
     *
     * @return 남긴 개정 이력. 남길 것이 없으면 null
     */
    @Transactional
    public LawRevision syncOne(TrackedLaw law, LawApiClient client) {
        boolean isFirstCheck = law.getLastSyncedAt() == null;
        LocalDateTime now = Times.nowUtc();

        List<LawApiItem> results;
        try {
            results = client.search(law.getSourceType(), law.getName());
        } catch (RuntimeException e) {
            // 조회에 실패해도 "확인을 시도한 시각"은 남긴다. 그래야 실패가
            // 이어져도 매번 첫 확인으로 취급되지 않는다.
            law.setLastSyncedAt(now);
            law.setUpdatedAt(now);
            trackedLawMapper.updateCurrentState(law);
            throw e;
        }

        law.setLastSyncedAt(now);
        law.setUpdatedAt(now);

        LawApiItem match = findMatch(law, results);
        if (match == null) {
            trackedLawMapper.updateCurrentState(law);
            return null;
        }

        LawRevision revision = null;
        if (isFirstCheck) {
            if (match.getPromulgationDate() != null || match.getEnforcementDate() != null) {
                revision = newRevision(law, match, null, null, null, now);
            }
        } else if (changed(law, match)) {
            revision = newRevision(law, match,
                    law.getCurrentPromulgationNo(),
                    law.getCurrentPromulgationDate(),
                    law.getCurrentEnforcementDate(),
                    now);
        }

        // 값이 있는 항목만 덮어쓴다. 응답에서 빠진 항목 때문에 기존 값이
        // 지워지면 다음 확인에서 거짓 개정으로 잡힌다.
        if (match.getMasterId() != null) {
            law.setMasterId(match.getMasterId());
        }
        if (match.getExternalId() != null) {
            law.setExternalId(match.getExternalId());
        }
        if (match.getPromulgationNo() != null) {
            law.setCurrentPromulgationNo(match.getPromulgationNo());
        }
        if (match.getPromulgationDate() != null) {
            law.setCurrentPromulgationDate(match.getPromulgationDate());
        }
        if (match.getEnforcementDate() != null) {
            law.setCurrentEnforcementDate(match.getEnforcementDate());
        }
        if (match.getDetailLink() != null) {
            law.setDetailLink(match.getDetailLink());
        }
        trackedLawMapper.updateCurrentState(law);

        if (revision != null) {
            revisionMapper.insert(revision);
        }
        return revision;
    }

    private LawRevision newRevision(TrackedLaw law, LawApiItem match,
                                    String previousNo, String previousDate, String previousEnforcement,
                                    LocalDateTime now) {
        LawRevision revision = new LawRevision();
        revision.setTrackedLawId(law.getId());
        revision.setPromulgationNo(match.getPromulgationNo());
        revision.setPromulgationDate(match.getPromulgationDate());
        revision.setEnforcementDate(match.getEnforcementDate());
        revision.setPreviousPromulgationNo(previousNo);
        revision.setPreviousPromulgationDate(previousDate);
        revision.setPreviousEnforcementDate(previousEnforcement);
        revision.setDetectedAt(now);
        revision.setReviewStatus(STATUS_UNREVIEWED);
        revision.setRawData(toJson(match));
        return revision;
    }

    private String toJson(LawApiItem item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            // 원본 응답 보관은 부가 정보다. 실패해도 개정 감지 자체를 막지 않는다.
            log.warn("개정 이력의 원본 응답을 저장하지 못했습니다: {}", e.getMessage());
            return null;
        }
    }

    private boolean changed(TrackedLaw law, LawApiItem match) {
        return !Objects.equals(blankToNull(match.getPromulgationNo()), law.getCurrentPromulgationNo())
                || !Objects.equals(blankToNull(match.getPromulgationDate()), law.getCurrentPromulgationDate())
                || !Objects.equals(blankToNull(match.getEnforcementDate()), law.getCurrentEnforcementDate());
    }

    /**
     * 검색 결과 중 이 법령에 해당하는 항목을 고른다.
     *
     * <p>개정돼도 바뀌지 않는 식별자(masterId)를 가장 먼저 본다. 그래야
     * 일련번호 자체가 바뀐 것, 즉 실제 개정을 알아챌 수 있다. 그 다음은
     * 현재 저장된 일련번호, 마지막으로 이름이 정확히 같은 것 순이다.
     */
    private LawApiItem findMatch(TrackedLaw law, List<LawApiItem> results) {
        if (law.getMasterId() != null && !law.getMasterId().isBlank()) {
            for (LawApiItem r : results) {
                if (r.getMasterId() != null && r.getMasterId().equals(law.getMasterId())) {
                    return r;
                }
            }
        }
        for (LawApiItem r : results) {
            if (Objects.equals(r.getExternalId(), law.getExternalId())) {
                return r;
            }
        }
        for (LawApiItem r : results) {
            if (Objects.equals(r.getName(), law.getName())) {
                return r;
            }
        }
        return null;
    }

    // ---------- 신규 제정 고시 탐지 ----------

    /**
     * 소관부처 + 키워드 이중 필터로 아직 등록하지 않은 고시/예규/훈령을 찾는다.
     *
     * <p>고시는 기존 것이 개정되는 게 아니라 매년 새로 제정되는 경우가 많다.
     * 그런 항목은 완전히 다른 일련번호를 가진 별개 항목이 되므로, 등록된
     * 항목의 변경만 확인하는 방식으로는 발견할 수 없다.
     *
     * <p>이미 후보로 저장해 둔 것(무시한 것 포함)은 다시 만들지 않는다.
     * 한 번 무시하면 그 항목은 다시 떠오르지 않는다.
     */
    @Transactional
    public List<NewAdmrulCandidate> scanNewAdmrul(LawApiClient client, List<String> keywords,
                                                  String department, String sinceDate) {
        // 새로 훑기 전에 지금 조건에 맞지 않는 예전 후보부터 지운다. 그래야
        // 지운 항목이 지금 조건에는 맞는 것으로 밝혀졌을 때 아래에서 다시
        // 정상적으로 발견된다.
        pruneStaleCandidates(keywords, department, sinceDate);
        pruneOrphanedRegisteredCandidates();

        Set<String> trackedKeys = activeTrackedAdmrulKeys();
        Set<String> existingKeys = new HashSet<>();
        for (NewAdmrulCandidate c : candidateMapper.findAll()) {
            existingKeys.add(key(c.getSourceType(), c.getExternalId()));
        }

        List<NewAdmrulCandidate> created = new ArrayList<>();
        LocalDateTime now = Times.nowUtc();

        for (String keyword : keywords) {
            boolean reachedLastPage = false;
            for (int page = 1; page <= SCAN_MAX_PAGES; page++) {
                List<LawApiItem> results;
                try {
                    results = client.search(LawApiClient.ADMRUL, keyword, SCAN_PAGE_SIZE, page);
                } catch (LawApiException e) {
                    reachedLastPage = true;
                    break;
                }
                if (results.isEmpty()) {
                    reachedLastPage = true;
                    break;
                }

                for (LawApiItem r : results) {
                    String sourceType = r.getSourceType() == null ? LawApiClient.ADMRUL : r.getSourceType();
                    String externalId = r.getExternalId();
                    if (externalId == null || externalId.isBlank()) {
                        continue;
                    }
                    String key = key(sourceType, externalId);
                    if (trackedKeys.contains(key) || existingKeys.contains(key)) {
                        continue;
                    }
                    if (!matchesDepartment(department, r.getDepartment())) {
                        continue;
                    }
                    if (!matchesSinceDate(sinceDate, r.getPromulgationDate())) {
                        continue;
                    }

                    NewAdmrulCandidate candidate = new NewAdmrulCandidate();
                    candidate.setSourceType(sourceType);
                    candidate.setExternalId(externalId);
                    candidate.setMasterId(r.getMasterId());
                    candidate.setName(r.getName() == null ? "" : r.getName());
                    candidate.setCategory(r.getCategory());
                    candidate.setDepartment(r.getDepartment());
                    candidate.setPromulgationNo(r.getPromulgationNo());
                    candidate.setPromulgationDate(r.getPromulgationDate());
                    candidate.setEnforcementDate(r.getEnforcementDate());
                    candidate.setDetailLink(r.getDetailLink());
                    candidate.setMatchedKeyword(keyword);
                    candidate.setStatus(CANDIDATE_NEW);
                    candidate.setFirstSeenAt(now);

                    candidateMapper.insert(candidate);
                    existingKeys.add(key);
                    created.add(candidate);
                }

                if (results.size() < SCAN_PAGE_SIZE) {
                    reachedLastPage = true;
                    break;
                }
            }
            if (!reachedLastPage) {
                log.warn("신규 제정 고시 탐지: 키워드 '{}'가 최대 페이지 수({})에 도달해 중단됨(더 남아있을 수 있음)",
                        keyword, SCAN_MAX_PAGES);
            }
        }
        return created;
    }

    /**
     * 설정을 바꿨을 때, 이미 찾아 둔 "신규" 후보 중 새 조건에 더는 맞지 않는
     * 것을 지운다.
     *
     * <p>지우지 않으면 예전 조건으로 찾은 항목이 화면에 계속 섞여 보여서,
     * 설정을 바꿔도 목록이 그대로인 것처럼 보인다. 이미 등록했거나 무시한
     * 항목은 화면에 나오지 않으므로 건드리지 않는다.
     */
    @Transactional
    public int pruneStaleCandidates(List<String> keywords, String department, String sinceDate) {
        List<Long> toRemove = new ArrayList<>();
        for (NewAdmrulCandidate c : candidateMapper.findByStatus(CANDIDATE_NEW, Integer.MAX_VALUE)) {
            if (!matchesCurrentFilter(c, keywords, department, sinceDate)) {
                toRemove.add(c.getId());
            }
        }
        if (!toRemove.isEmpty()) {
            candidateMapper.deleteByIds(toRemove);
        }
        return toRemove.size();
    }

    /**
     * 추적 중이지 않은데 "등록됨"으로 남아 있는 후보를 지운다.
     *
     * <p>후보에서 등록을 누르면 상태가 "등록됨"이 되고, 다음 탐지에서 다시
     * 후보로 뜨지 않도록 그 행이 기준으로 쓰인다. 그런데 그 뒤 법령 목록에서
     * 해당 법령을 삭제하면 추적 대상이 아닌데도 "등록됨" 행만 영원히 남아,
     * 다시 검색해도 후보로 떠오르지 않게 된다. 화면 어디에도 보이지 않아
     * 원인을 알기도 어렵다.
     */
    @Transactional
    public int pruneOrphanedRegisteredCandidates() {
        Set<String> trackedKeys = activeTrackedAdmrulKeys();
        List<Long> toRemove = new ArrayList<>();
        for (NewAdmrulCandidate c : candidateMapper.findByStatus(CANDIDATE_REGISTERED, Integer.MAX_VALUE)) {
            if (!trackedKeys.contains(key(c.getSourceType(), c.getExternalId()))) {
                toRemove.add(c.getId());
            }
        }
        if (!toRemove.isEmpty()) {
            candidateMapper.deleteByIds(toRemove);
        }
        return toRemove.size();
    }

    /**
     * 이 후보가 지금 설정된 조건에 여전히 맞는지, 저장된 값만으로 판단한다.
     *
     * <p>소관부처나 공포일자 정보가 아예 없는 항목은 걸러내지 않는다.
     * API 응답에서 그 항목을 읽지 못했을 뿐일 수 있는데, 없다고 걸러내면
     * 실제로는 조건에 맞는 항목을 영영 놓치게 된다.
     */
    private boolean matchesCurrentFilter(NewAdmrulCandidate candidate, List<String> keywords,
                                         String department, String sinceDate) {
        if (!matchesDepartment(department, candidate.getDepartment())) {
            return false;
        }
        if (keywords != null && !keywords.isEmpty()) {
            String name = candidate.getName() == null ? "" : candidate.getName();
            boolean hit = keywords.stream().anyMatch(name::contains);
            if (!hit) {
                return false;
            }
        }
        return matchesSinceDate(sinceDate, candidate.getPromulgationDate());
    }

    private boolean matchesDepartment(String wanted, String actual) {
        if (wanted == null || wanted.isBlank() || actual == null || actual.isBlank()) {
            return true;
        }
        return actual.contains(wanted);
    }

    private boolean matchesSinceDate(String sinceDate, String promulgationDate) {
        if (sinceDate == null || sinceDate.isBlank()
                || promulgationDate == null || promulgationDate.isBlank()) {
            return true;
        }
        return promulgationDate.compareTo(sinceDate) >= 0;
    }

    /**
     * 활성 상태로 추적 중인 행정규칙의 키 집합.
     *
     * <p>탐지할 때와 유령 후보를 정리할 때 반드시 같은 기준을 써야 한다.
     * 한쪽에 활성 조건이 빠지면, 삭제한 법령이 한쪽에서는 영원히 "추적 중"으로
     * 남아 다시는 탐지되지 않는다.
     */
    private Set<String> activeTrackedAdmrulKeys() {
        Set<String> keys = new HashSet<>();
        for (TrackedLaw law : trackedLawMapper.findActiveBySourceType(LawApiClient.ADMRUL)) {
            keys.add(key(law.getSourceType(), law.getExternalId()));
        }
        return keys;
    }

    private static String key(String sourceType, String externalId) {
        return sourceType + " " + externalId;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
