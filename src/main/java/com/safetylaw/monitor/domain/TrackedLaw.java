package com.safetylaw.monitor.domain;

import java.time.LocalDateTime;

/**
 * 추적 대상으로 등록해 둔 법령 또는 행정규칙(고시/예규/훈령). 테이블 TRACKED_LAWS.
 *
 * <p>{@code externalId} 는 특정 공포 버전을 가리키는 값이라 개정되면 바뀌고,
 * {@code masterId} 는 개정과 무관하게 고정되는 식별자다. 개정 감지는 이 둘의
 * 차이를 이용한다.
 *
 * <p>모든 시각은 UTC 로 저장한다({@code Times.nowUtc()}).
 */
public class TrackedLaw {

    private Long id;
    private String sourceType;
    private String externalId;
    private String masterId;
    private String name;
    private String category;
    private String department;
    private String currentPromulgationNo;
    private String currentPromulgationDate;
    private String currentEnforcementDate;
    private String detailLink;
    private Boolean isActive = Boolean.TRUE;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public String getCurrentPromulgationNo() {
        return currentPromulgationNo;
    }

    public void setCurrentPromulgationNo(String currentPromulgationNo) {
        this.currentPromulgationNo = currentPromulgationNo;
    }

    public String getCurrentPromulgationDate() {
        return currentPromulgationDate;
    }

    public void setCurrentPromulgationDate(String currentPromulgationDate) {
        this.currentPromulgationDate = currentPromulgationDate;
    }

    public String getCurrentEnforcementDate() {
        return currentEnforcementDate;
    }

    public void setCurrentEnforcementDate(String currentEnforcementDate) {
        this.currentEnforcementDate = currentEnforcementDate;
    }

    public String getDetailLink() {
        return detailLink;
    }

    public void setDetailLink(String detailLink) {
        this.detailLink = detailLink;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
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
