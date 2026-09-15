package com.safetylaw.monitor.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.safetylaw.monitor.support.Times;

/**
 * 화면에 내보내는 개정 이력.
 *
 * <p>시각은 반드시 UTC 오프셋이 붙은 형태로 나가야 한다. 오프셋 없이
 * 내보내면 브라우저가 현지 시각으로 해석해 9시간 어긋나게 표시된다.
 *
 * @param matchedBy 이 항목이 문서 검토 목록에 뜬 이유.
 *                  "mapping"(직접 매핑해 둔 법령) 또는 "tag"(문서 키워드가
 *                  법령명·본문과 겹쳐 자동으로 걸린 경우). 문서 기준 화면에서만
 *                  의미가 있고 그 밖의 목록에서는 기본값 그대로 무시하면 된다.
 */
public record LawRevisionResponse(
        Long id,
        Long trackedLawId,
        String trackedLawName,
        String trackedLawCategory,
        String promulgationNo,
        String promulgationDate,
        String enforcementDate,
        String previousPromulgationNo,
        String previousPromulgationDate,
        String previousEnforcementDate,
        OffsetDateTime detectedAt,
        String reviewStatus,
        String reviewer,
        OffsetDateTime reviewedAt,
        String note,
        List<String> mappedDocuments,
        String matchedBy) {

    public static final String MATCHED_BY_MAPPING = "mapping";
    public static final String MATCHED_BY_TAG = "tag";

    public static LawRevisionResponse of(LawRevisionRow row, List<String> mappedDocuments, String matchedBy) {
        return new LawRevisionResponse(
                row.getId(),
                row.getTrackedLawId(),
                row.getTrackedLawName() == null ? "" : row.getTrackedLawName(),
                row.getTrackedLawCategory(),
                row.getPromulgationNo(),
                row.getPromulgationDate(),
                row.getEnforcementDate(),
                row.getPreviousPromulgationNo(),
                row.getPreviousPromulgationDate(),
                row.getPreviousEnforcementDate(),
                Times.toUtcOffset(row.getDetectedAt()),
                row.getReviewStatus(),
                row.getReviewer(),
                Times.toUtcOffset(row.getReviewedAt()),
                row.getNote(),
                mappedDocuments == null ? List.of() : mappedDocuments,
                matchedBy == null ? MATCHED_BY_MAPPING : matchedBy);
    }
}
