package com.safetylaw.monitor.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.safetylaw.monitor.domain.CompanyDocument;
import com.safetylaw.monitor.support.Times;

/** 화면에 내보내는 사내 문서. */
public record CompanyDocumentResponse(
        Long id,
        String docType,
        String docNumber,
        String title,
        String revisionNo,
        String revisionDate,
        String owner,
        String fileLink,
        String note,
        String tags,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        int mappedLawCount,
        List<String> mappedLaws) {

    public static CompanyDocumentResponse of(CompanyDocument document, List<String> mappedLaws) {
        List<String> laws = mappedLaws == null ? List.of() : mappedLaws;
        return new CompanyDocumentResponse(
                document.getId(),
                document.getDocType(),
                document.getDocNumber(),
                document.getTitle(),
                document.getRevisionNo(),
                document.getRevisionDate(),
                document.getOwner(),
                document.getFileLink(),
                document.getNote(),
                document.getTags(),
                Times.toUtcOffset(document.getCreatedAt()),
                Times.toUtcOffset(document.getUpdatedAt()),
                laws.size(),
                laws);
    }
}
