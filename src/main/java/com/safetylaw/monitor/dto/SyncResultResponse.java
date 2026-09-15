package com.safetylaw.monitor.dto;

import java.util.List;

/** 동기화 실행 결과. */
public record SyncResultResponse(
        int checked,
        int newRevisions,
        int newAdmrulCandidates,
        List<String> errors) {
}
