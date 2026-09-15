package com.safetylaw.monitor.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/** 개정 이력 화면에서 보내는 요청들. */
public final class RevisionRequests {

    private RevisionRequests() {
    }

    /** 값이 null 인 항목은 바꾸지 않는다는 뜻이다. */
    public record Update(String reviewStatus, String reviewer, String note) {
    }

    public record BulkStatus(@NotEmpty(message = "선택된 항목이 없습니다.") List<Long> ids,
                             String reviewStatus) {
    }

    public record BulkIds(@NotEmpty(message = "선택된 항목이 없습니다.") List<Long> ids) {
    }
}
