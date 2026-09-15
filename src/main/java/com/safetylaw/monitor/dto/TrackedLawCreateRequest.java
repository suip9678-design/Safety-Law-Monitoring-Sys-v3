package com.safetylaw.monitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 검색 결과에서 고른 항목을 추적 대상으로 등록할 때 보내는 값. */
public record TrackedLawCreateRequest(
        @NotBlank
        @Pattern(regexp = "law|admrul", message = "source_type은 law 또는 admrul 이어야 합니다.")
        String sourceType,

        @NotBlank String externalId,
        String masterId,
        @NotBlank String name,
        String category,
        String department,
        String promulgationNo,
        String promulgationDate,
        String enforcementDate,
        String detailLink) {
}
