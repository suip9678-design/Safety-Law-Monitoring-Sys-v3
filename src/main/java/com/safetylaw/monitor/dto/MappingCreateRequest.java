package com.safetylaw.monitor.dto;

import jakarta.validation.constraints.NotNull;

/** 사내 문서와 법령을 잇는 요청. */
public record MappingCreateRequest(
        @NotNull Long documentId,
        @NotNull Long trackedLawId,
        String note) {
}
