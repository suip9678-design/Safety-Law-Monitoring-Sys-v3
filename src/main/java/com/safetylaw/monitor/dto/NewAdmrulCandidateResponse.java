package com.safetylaw.monitor.dto;

import java.time.OffsetDateTime;

import com.safetylaw.monitor.domain.NewAdmrulCandidate;
import com.safetylaw.monitor.support.Times;

/** 화면에 내보내는 신규 제정 고시 후보. */
public record NewAdmrulCandidateResponse(
        Long id,
        String sourceType,
        String externalId,
        String masterId,
        String name,
        String category,
        String department,
        String promulgationNo,
        String promulgationDate,
        String enforcementDate,
        String detailLink,
        String matchedKeyword,
        OffsetDateTime firstSeenAt) {

    public static NewAdmrulCandidateResponse of(NewAdmrulCandidate candidate) {
        return new NewAdmrulCandidateResponse(
                candidate.getId(),
                candidate.getSourceType(),
                candidate.getExternalId(),
                candidate.getMasterId(),
                candidate.getName(),
                candidate.getCategory(),
                candidate.getDepartment(),
                candidate.getPromulgationNo(),
                candidate.getPromulgationDate(),
                candidate.getEnforcementDate(),
                candidate.getDetailLink(),
                candidate.getMatchedKeyword(),
                Times.toUtcOffset(candidate.getFirstSeenAt()));
    }
}
