package com.safetylaw.monitor.dto;

/** 화면에 내보내는 문서-법령 매핑. */
public record MappingResponse(
        Long id,
        Long documentId,
        String documentTitle,
        Long trackedLawId,
        String trackedLawName,
        String note) {

    public static MappingResponse of(MappingRow row) {
        return new MappingResponse(
                row.getId(),
                row.getDocumentId(),
                row.getDocumentTitle() == null ? "" : row.getDocumentTitle(),
                row.getTrackedLawId(),
                row.getTrackedLawName() == null ? "" : row.getTrackedLawName(),
                row.getNote());
    }
}
