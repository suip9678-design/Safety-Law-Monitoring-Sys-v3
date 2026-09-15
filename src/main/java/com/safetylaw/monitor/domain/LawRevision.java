package com.safetylaw.monitor.domain;

import java.time.LocalDateTime;

/**
 * 감지된 개정 이력 한 건. 테이블 LAW_REVISIONS.
 *
 * <p>동기화 때 등록된 법령의 공포번호/공포일자/시행일자가 이전 값과
 * 달라지면 한 행이 쌓인다. previous* 컬럼에 직전 값을 함께 남겨
 * 무엇이 어떻게 바뀌었는지 화면에서 바로 비교할 수 있게 한다.
 */
public class LawRevision {

    private Long id;
    private Long trackedLawId;
    private String promulgationNo;
    private String promulgationDate;
    private String enforcementDate;
    private String previousPromulgationNo;
    private String previousPromulgationDate;
    private String previousEnforcementDate;
    private LocalDateTime detectedAt;
    private String reviewStatus;
    private String reviewer;
    private LocalDateTime reviewedAt;
    private String note;
    private String rawData;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTrackedLawId() {
        return trackedLawId;
    }

    public void setTrackedLawId(Long trackedLawId) {
        this.trackedLawId = trackedLawId;
    }

    public String getPromulgationNo() {
        return promulgationNo;
    }

    public void setPromulgationNo(String promulgationNo) {
        this.promulgationNo = promulgationNo;
    }

    public String getPromulgationDate() {
        return promulgationDate;
    }

    public void setPromulgationDate(String promulgationDate) {
        this.promulgationDate = promulgationDate;
    }

    public String getEnforcementDate() {
        return enforcementDate;
    }

    public void setEnforcementDate(String enforcementDate) {
        this.enforcementDate = enforcementDate;
    }

    public String getPreviousPromulgationNo() {
        return previousPromulgationNo;
    }

    public void setPreviousPromulgationNo(String previousPromulgationNo) {
        this.previousPromulgationNo = previousPromulgationNo;
    }

    public String getPreviousPromulgationDate() {
        return previousPromulgationDate;
    }

    public void setPreviousPromulgationDate(String previousPromulgationDate) {
        this.previousPromulgationDate = previousPromulgationDate;
    }

    public String getPreviousEnforcementDate() {
        return previousEnforcementDate;
    }

    public void setPreviousEnforcementDate(String previousEnforcementDate) {
        this.previousEnforcementDate = previousEnforcementDate;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setReviewer(String reviewer) {
        this.reviewer = reviewer;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }
}
