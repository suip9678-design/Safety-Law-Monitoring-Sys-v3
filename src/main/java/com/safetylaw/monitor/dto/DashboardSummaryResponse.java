package com.safetylaw.monitor.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 대시보드 요약.
 *
 * @param recentRevisions        아직 확인이 필요한 개정(법령 축)
 * @param recentDocumentImpacts  검토가 필요한 사내 문서(문서 축)
 * @param newAdmrulCandidates    아직 등록하지 않은 신규 제정 고시 후보
 */
public record DashboardSummaryResponse(
        int trackedLawCount,
        int unreviewedCount,
        int inReviewCount,
        int reflectedCount,
        int documentCount,
        int unmappedLawCount,
        OffsetDateTime lastSyncAt,
        List<LawRevisionResponse> recentRevisions,
        List<DocumentImpactResponse> recentDocumentImpacts,
        List<NewAdmrulCandidateResponse> newAdmrulCandidates) {
}
