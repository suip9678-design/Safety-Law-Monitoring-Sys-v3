package com.safetylaw.monitor.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safetylaw.monitor.domain.NewAdmrulCandidate;
import com.safetylaw.monitor.domain.TrackedLaw;
import com.safetylaw.monitor.dto.LawSearchResultResponse;
import com.safetylaw.monitor.dto.NewAdmrulCandidateResponse;
import com.safetylaw.monitor.dto.TrackedLawCreateRequest;
import com.safetylaw.monitor.dto.TrackedLawResponse;
import com.safetylaw.monitor.dto.TrackedLawRow;
import com.safetylaw.monitor.lawapi.LawApiClient;
import com.safetylaw.monitor.lawapi.LawApiClientFactory;
import com.safetylaw.monitor.lawapi.LawApiException;
import com.safetylaw.monitor.lawapi.LawApiItem;
import com.safetylaw.monitor.mapper.NewAdmrulCandidateMapper;
import com.safetylaw.monitor.mapper.TrackedLawMapper;
import com.safetylaw.monitor.support.Times;
import com.safetylaw.monitor.web.NotFoundException;

/** 추적 법령 등록·조회와 신규 제정 고시 후보 처리. */
@Service
public class LawService {

    private static final Logger log = LoggerFactory.getLogger(LawService.class);

    private static final int CANDIDATE_LIST_LIMIT = 500;

    private final TrackedLawMapper trackedLawMapper;
    private final NewAdmrulCandidateMapper candidateMapper;
    private final LawApiClientFactory clientFactory;
    private final SettingsService settings;
    private final SyncService syncService;
    private final ContentCacheService contentCacheService;

    public LawService(TrackedLawMapper trackedLawMapper,
                      NewAdmrulCandidateMapper candidateMapper,
                      LawApiClientFactory clientFactory,
                      SettingsService settings,
                      SyncService syncService,
                      ContentCacheService contentCacheService) {
        this.trackedLawMapper = trackedLawMapper;
        this.candidateMapper = candidateMapper;
        this.clientFactory = clientFactory;
        this.settings = settings;
        this.syncService = syncService;
        this.contentCacheService = contentCacheService;
    }

    /** 이미 추적 중인 항목을 다시 등록하려 할 때. */
    public static class AlreadyTrackedException extends RuntimeException {
        public AlreadyTrackedException(String message) {
            super(message);
        }
    }

    public List<LawSearchResultResponse> search(String sourceType, String query) {
        LawApiClient client = clientFactory.create(settings.get(SettingsService.LAW_API_OC));
        List<LawSearchResultResponse> out = new ArrayList<>();
        for (LawApiItem item : client.search(sourceType, query)) {
            out.add(LawSearchResultResponse.of(item));
        }
        return out;
    }

    public List<TrackedLawResponse> list(boolean activeOnly, String sourceType) {
        List<TrackedLawResponse> out = new ArrayList<>();
        for (TrackedLawRow row : trackedLawMapper.findAllWithCounts(sourceType, activeOnly)) {
            out.add(TrackedLawResponse.of(row));
        }
        return out;
    }

    public TrackedLawResponse get(Long id) {
        TrackedLawRow row = trackedLawMapper.findRowById(id);
        if (row == null) {
            throw new NotFoundException("추적 중인 법령을 찾을 수 없습니다.");
        }
        return TrackedLawResponse.of(row);
    }

    /**
     * 추적 대상으로 등록한다.
     *
     * <p>등록 직후 한 번 확인해 "미검토" 이력을 남긴다. 추적을 시작하는 것
     * 자체가 한 번은 살펴봐야 할 일이기 때문이다.
     */
    public TrackedLawResponse register(TrackedLawCreateRequest request) {
        TrackedLaw law = saveForTracking(request);

        // 확인과 본문 받기는 등록 자체와 분리한다. 외부 API 가 실패해도
        // 등록은 이미 끝난 상태여야 한다.
        try {
            LawApiClient client = clientFactory.create(settings.get(SettingsService.LAW_API_OC));
            syncService.syncOne(law, client);
            contentCacheService.refreshTrackedLawContent(client);
        } catch (LawApiException e) {
            log.warn("등록 직후 확인에 실패했습니다({}). 다음 동기화 때 다시 시도됩니다: {}",
                    request.name(), e.getMessage());
        }

        return get(law.getId());
    }

    // 쓰기가 한 건(추가 또는 수정)뿐이라 그 자체로 원자적이다. 별도 트랜잭션을
    // 걸지 않는다. 같은 클래스 안에서 부르는 메서드에는 어차피 트랜잭션이
    // 적용되지 않으므로, 걸어 두면 동작하는 것처럼 보이기만 해서 더 위험하다.
    private TrackedLaw saveForTracking(TrackedLawCreateRequest request) {
        TrackedLaw existing = trackedLawMapper.findBySourceAndExternalId(
                request.sourceType(), request.externalId());

        if (existing != null && Boolean.TRUE.equals(existing.getIsActive())) {
            throw new AlreadyTrackedException("이미 추적 중인 법령/고시입니다.");
        }

        if (existing != null) {
            // 예전에 추적하다 삭제한 법령을 다시 등록하는 경우다. 그 사이 실제
            // 개정이 있었을 수 있으므로 최초 등록과 똑같이 취급한다. 마지막
            // 확인 시각을 비워 두면 다음 확인이 "최초 확인"으로 처리되어
            // 미검토 이력이 다시 생긴다.
            existing.setIsActive(true);
            existing.setMasterId(request.masterId());
            existing.setName(request.name());
            existing.setCategory(request.category());
            existing.setDepartment(request.department());
            existing.setCurrentPromulgationNo(request.promulgationNo());
            existing.setCurrentPromulgationDate(request.promulgationDate());
            existing.setCurrentEnforcementDate(request.enforcementDate());
            existing.setDetailLink(request.detailLink());
            existing.setLastSyncedAt(null);
            existing.setUpdatedAt(Times.nowUtc());
            trackedLawMapper.updateCurrentState(existing);
            return existing;
        }

        TrackedLaw law = new TrackedLaw();
        law.setSourceType(request.sourceType());
        law.setExternalId(request.externalId());
        law.setMasterId(request.masterId());
        law.setName(request.name());
        law.setCategory(request.category());
        law.setDepartment(request.department());
        // 검색 결과에서 이미 확인된 값으로 바로 채운다. 상세 재조회가 실패해도
        // 화면에 빈 값이 남지 않게 하기 위함이다.
        law.setCurrentPromulgationNo(request.promulgationNo());
        law.setCurrentPromulgationDate(request.promulgationDate());
        law.setCurrentEnforcementDate(request.enforcementDate());
        law.setDetailLink(request.detailLink());
        law.setIsActive(true);
        law.setCreatedAt(Times.nowUtc());
        law.setUpdatedAt(Times.nowUtc());
        trackedLawMapper.insert(law);
        return law;
    }

    /**
     * 추적을 그만둔다.
     *
     * <p>행을 지우지 않고 비활성으로 표시한다. 지금까지 쌓인 개정 이력은
     * 기록으로 남아야 하기 때문이다.
     */
    @Transactional
    public void deactivate(Long id) {
        TrackedLaw law = trackedLawMapper.findById(id);
        if (law == null) {
            throw new NotFoundException("추적 중인 법령을 찾을 수 없습니다.");
        }
        law.setIsActive(false);
        law.setUpdatedAt(Times.nowUtc());
        trackedLawMapper.updateCurrentState(law);
    }

    // ---------- 신규 제정 고시 후보 ----------

    public List<NewAdmrulCandidateResponse> dismissedCandidates() {
        List<NewAdmrulCandidateResponse> out = new ArrayList<>();
        for (NewAdmrulCandidate candidate :
                candidateMapper.findByStatus(SyncService.CANDIDATE_DISMISSED, CANDIDATE_LIST_LIMIT)) {
            out.add(NewAdmrulCandidateResponse.of(candidate));
        }
        return out;
    }

    @Transactional
    public void updateCandidateStatus(Long candidateId, String status) {
        if (candidateMapper.findById(candidateId) == null) {
            throw new NotFoundException("후보를 찾을 수 없습니다.");
        }
        candidateMapper.updateStatus(candidateId, status);
    }

    /** 무시해 둔 후보를 다시 신규로 되돌린다. */
    @Transactional
    public void bulkRestoreCandidates(List<Long> ids) {
        List<Long> dismissed = new ArrayList<>();
        for (NewAdmrulCandidate candidate :
                candidateMapper.findByStatus(SyncService.CANDIDATE_DISMISSED, CANDIDATE_LIST_LIMIT)) {
            if (ids.contains(candidate.getId())) {
                dismissed.add(candidate.getId());
            }
        }
        if (!dismissed.isEmpty()) {
            candidateMapper.bulkUpdateStatus(dismissed, SyncService.CANDIDATE_NEW);
        }
    }
}
