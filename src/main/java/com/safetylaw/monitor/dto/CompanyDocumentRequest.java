package com.safetylaw.monitor.dto;

import jakarta.validation.constraints.NotBlank;

/** 사내 문서를 등록하거나 수정할 때 보내는 값. */
public record CompanyDocumentRequest(
        String docType,
        String docNumber,
        @NotBlank String title,
        String revisionNo,
        String revisionDate,
        String owner,
        String fileLink,
        String note,
        /** 쉼표로 구분한 키워드. '#' 없이 보낸다. */
        String tags) {

    public String docTypeOrDefault() {
        return (docType == null || docType.isBlank()) ? "절차서" : docType;
    }
}
