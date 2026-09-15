package com.safetylaw.monitor.lawapi;

/**
 * 국가법령정보 API 응답 한 건에서 뽑아낸 값.
 *
 * <p>목록 조회 결과와 상세 조회 결과를 같은 형태로 담는다. 목록 조회에는
 * 본문({@code content})이 없고 상세 조회에만 채워진다.
 */
public class LawApiItem {

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

    /** 상세 조회에서만 채워지는 조문 본문. */
    private String content;

    /**
     * 본문 앞부분 중 조번호 라벨이 붙었을 수 있는 구간의 길이.
     *
     * <p>검색어가 이 구간 밖(별표/서식/부칙 등)에서 걸리면 조번호를 알 수
     * 없으므로 조문 링크를 만들지 않는다.
     */
    private int articleContentLen;

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

    public int getArticleContentLen() {
        return articleContentLen;
    }

    public void setArticleContentLen(int articleContentLen) {
        this.articleContentLen = articleContentLen;
    }
}
