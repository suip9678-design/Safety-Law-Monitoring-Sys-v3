package com.safetylaw.monitor.domain;

import java.time.LocalDateTime;

/**
 * 법령/행정규칙 본문(조문) 캐시. 테이블 SCRAPED_LAW_CONTENTS.
 *
 * <p>국가법령정보 공동활용 API 의 목록 조회는 법령명 검색만 지원하고
 * 본문 전체를 대상으로 한 검색은 제공하지 않는다(본문은 상세 조회로
 * 한 건씩만 받을 수 있음). 그래서 본문 검색과 사규 태그 매칭 모두
 * 미리 받아 둔 이 캐시를 대상으로 로컬에서 수행한다.
 *
 * <p>{@code articleContentLen} 은 본문 앞부분 중 조번호 라벨이 붙어
 * 있을 수 있는 구간의 길이다. 검색어가 이 구간 밖(별표/서식/부칙 등)에서
 * 걸리면 조번호를 알 수 없어 조문 링크를 만들지 않는다.
 */
public class ScrapedLawContent {

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
    private String content;
    private Integer articleContentLen;
    private LocalDateTime cachedAt;

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getArticleContentLen() {
        return articleContentLen;
    }

    public void setArticleContentLen(Integer articleContentLen) {
        this.articleContentLen = articleContentLen;
    }

    public LocalDateTime getCachedAt() {
        return cachedAt;
    }

    public void setCachedAt(LocalDateTime cachedAt) {
        this.cachedAt = cachedAt;
    }
}
