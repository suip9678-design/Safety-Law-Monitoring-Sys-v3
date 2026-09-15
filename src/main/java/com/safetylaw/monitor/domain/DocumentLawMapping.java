package com.safetylaw.monitor.domain;

import java.time.LocalDateTime;

/**
 * 사내 문서와 법령을 수동으로 이어 둔 매핑. 테이블 DOC_LAW_MAPPINGS.
 *
 * <p>이 매핑이 있으면 해당 법령이 개정될 때 어떤 문서를 검토해야
 * 하는지 대시보드가 바로 보여준다.
 */
public class DocumentLawMapping {

    private Long id;
    private Long documentId;
    private Long trackedLawId;
    private String note;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Long getTrackedLawId() {
        return trackedLawId;
    }

    public void setTrackedLawId(Long trackedLawId) {
        this.trackedLawId = trackedLawId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
