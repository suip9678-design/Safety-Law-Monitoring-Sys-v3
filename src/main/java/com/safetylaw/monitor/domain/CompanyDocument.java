package com.safetylaw.monitor.domain;

import java.time.LocalDateTime;

/**
 * 사내 절차서/지침서/작업표준 한 건. 테이블 COMPANY_DOCUMENTS.
 *
 * <p>{@code tags} 는 쉼표로 구분한 키워드 목록이다('#' 없이 저장).
 * 명시적으로 매핑해 두지 않은 법령이라도 이 키워드가 법령명이나
 * 본문 캐시에 들어 있으면 그 개정을 이 문서의 검토 필요 목록에
 * 자동으로 띄운다.
 *
 * <p>{@code owner}(담당자)는 DB 에서 DOC_OWNER 컬럼에 대응한다.
 */
public class CompanyDocument {

    private Long id;
    private String docType;
    private String docNumber;
    private String title;
    private String revisionNo;
    private String revisionDate;
    private String owner;
    private String fileLink;
    private String note;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getDocNumber() {
        return docNumber;
    }

    public void setDocNumber(String docNumber) {
        this.docNumber = docNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRevisionNo() {
        return revisionNo;
    }

    public void setRevisionNo(String revisionNo) {
        this.revisionNo = revisionNo;
    }

    public String getRevisionDate() {
        return revisionDate;
    }

    public void setRevisionDate(String revisionDate) {
        this.revisionDate = revisionDate;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getFileLink() {
        return fileLink;
    }

    public void setFileLink(String fileLink) {
        this.fileLink = fileLink;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
