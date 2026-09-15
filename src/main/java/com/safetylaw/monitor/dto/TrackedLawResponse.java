package com.safetylaw.monitor.dto;

import java.time.OffsetDateTime;

import com.safetylaw.monitor.support.Times;

/** 화면에 내보내는 추적 법령. */
public record TrackedLawResponse(
        Long id,
        String sourceType,
        String externalId,
        String masterId,
        String name,
        String category,
        String department,
        String currentPromulgationNo,
        String currentPromulgationDate,
        String currentEnforcementDate,
        String detailLink,
        Boolean isActive,
        OffsetDateTime lastSyncedAt,
        int mappedDocumentCount,
        int unreviewedRevisionCount) {

    public static TrackedLawResponse of(TrackedLawRow row) {
        return new TrackedLawResponse(
                row.getId(),
                row.getSourceType(),
                row.getExternalId(),
                row.getMasterId(),
                row.getName(),
                row.getCategory(),
                row.getDepartment(),
                row.getCurrentPromulgationNo(),
                row.getCurrentPromulgationDate(),
                row.getCurrentEnforcementDate(),
                row.getDetailLink(),
                row.getIsActive(),
                Times.toUtcOffset(row.getLastSyncedAt()),
                row.getMappedDocumentCount(),
                row.getUnreviewedRevisionCount());
    }
}
