package com.safetylaw.monitor.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.safetylaw.monitor.domain.NewAdmrulCandidate;
import com.safetylaw.monitor.dto.DashboardSummaryResponse;
import com.safetylaw.monitor.dto.NewAdmrulCandidateResponse;
import com.safetylaw.monitor.dto.StatusCount;
import com.safetylaw.monitor.mapper.CompanyDocumentMapper;
import com.safetylaw.monitor.mapper.LawRevisionMapper;
import com.safetylaw.monitor.mapper.NewAdmrulCandidateMapper;
import com.safetylaw.monitor.mapper.TrackedLawMapper;
import com.safetylaw.monitor.support.Times;

/** 대시보드 요약 집계. */
@Service
public class DashboardService {

    /**
     * 대시보드 목록의 안전장치용 상한.
     *
     * <p>화면은 표 안에서 스크롤되도록 만들어져 있어 원래 의도는 "전부
     * 보여주기"다. 예전에 10건으로 묶여 있어 스크롤을 내려도 더 볼 것이
     * 없었던 적이 있다. 사실상 다 보이도록 넉넉히 두되, 쿼리가 무한정
     * 커지지는 않도록 상한은 남긴다.
     */
    private static final int LIST_LIMIT = 500;

    /** 대시보드에서 문서 한 건당 보여줄 개정 건수. */
    private static final int REVISIONS_PER_DOCUMENT = 5;

    private final TrackedLawMapper trackedLawMapper;
    private final LawRevisionMapper revisionMapper;
    private final CompanyDocumentMapper documentMapper;
    private final NewAdmrulCandidateMapper candidateMapper;
    private final RevisionService revisionService;
    private final DocumentImpactService impactService;

    public DashboardService(TrackedLawMapper trackedLawMapper,
                            LawRevisionMapper revisionMapper,
                            CompanyDocumentMapper documentMapper,
                            NewAdmrulCandidateMapper candidateMapper,
                            RevisionService revisionService,
                            DocumentImpactService impactService) {
        this.trackedLawMapper = trackedLawMapper;
        this.revisionMapper = revisionMapper;
        this.documentMapper = documentMapper;
        this.candidateMapper = candidateMapper;
        this.revisionService = revisionService;
        this.impactService = impactService;
    }

    public DashboardSummaryResponse summary() {
        // 비활성화한 법령의 개정 이력은 더 이상 추적 대상이 아니므로 요약에서
        // 제외한다. 개정 이력 탭에는 기록으로 계속 남는다.
        Map<String, Integer> statusCounts = new HashMap<>();
        for (StatusCount row : revisionMapper.countByStatusForActiveLaws()) {
            statusCounts.put(row.getStatus(), row.getCount());
        }

        List<NewAdmrulCandidateResponse> candidates = new ArrayList<>();
        for (NewAdmrulCandidate candidate :
                candidateMapper.findByStatus(SyncService.CANDIDATE_NEW, LIST_LIMIT)) {
            candidates.add(NewAdmrulCandidateResponse.of(candidate));
        }

        return new DashboardSummaryResponse(
                trackedLawMapper.countActive(),
                statusCounts.getOrDefault(SyncService.STATUS_UNREVIEWED, 0),
                statusCounts.getOrDefault(SyncService.STATUS_IN_REVIEW, 0),
                statusCounts.getOrDefault(SyncService.STATUS_REFLECTED, 0),
                documentMapper.countAll(),
                trackedLawMapper.countUnmapped(),
                Times.toUtcOffset(trackedLawMapper.findMaxLastSyncedAt()),
                revisionService.pendingForDashboard(LIST_LIMIT),
                // 이미 반영완료·해당없음으로 처리한 건은 "확인이 필요한 개정"이
                // 아니므로 문서 축 목록에서도 빠진다.
                impactService.compute(
                        List.of(SyncService.STATUS_UNREVIEWED, SyncService.STATUS_IN_REVIEW),
                        LIST_LIMIT, REVISIONS_PER_DOCUMENT),
                candidates);
    }
}
