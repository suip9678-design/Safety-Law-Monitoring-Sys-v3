package com.safetylaw.monitor.domain;

import java.time.LocalDateTime;

/**
 * 아직 등록하지 않은, 새로 제정된 것으로 보이는 고시/예규/훈령 후보.
 * 테이블 NEW_ADMRUL_CANDIDATES.
 *
 * <p>등록해 둔 고시가 "개정"되는 게 아니라 매년 새로 "제정"되는
 * 경우가 많아, 등록 항목의 변경만 추적해서는 이런 신규 제정을 놓친다.
 * 소관부처 + 키워드 이중 필터로 찾은 후보를 여기 쌓아 사용자가
 * 등록할지 무시할지 고르게 한다.
 *
 * <p>무시한 항목도 행을 남기고 {@code status} 만 바꾼다. 행을 지우면
 * 다음 스캔에서 같은 항목이 다시 후보로 떠오르기 때문이다.
 */
public class NewAdmrulCandidate {

    private Long id;
    private String sourceType;
    private String externalId;
    private String masterId;
    private String name;
    private String category;
    private String department;
    private String promulgationNo;
    private String promulgationDate;
    private String enforcementDate;
    private String detailLink;
    private String matchedKeyword;
    private String status;
    private LocalDateTime firstSeenAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getMasterId() {
        return masterId;
    }

    public void setMasterId(String masterId) {
        this.masterId = masterId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
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

    public String getDetailLink() {
        return detailLink;
    }

    public void setDetailLink(String detailLink) {
        this.detailLink = detailLink;
    }

    public String getMatchedKeyword() {
        return matchedKeyword;
    }

    public void setMatchedKeyword(String matchedKeyword) {
        this.matchedKeyword = matchedKeyword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(LocalDateTime firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }
}
