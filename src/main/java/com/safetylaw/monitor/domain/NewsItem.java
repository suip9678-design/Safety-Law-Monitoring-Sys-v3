package com.safetylaw.monitor.domain;

import java.time.LocalDateTime;

/**
 * 대시보드 자동 스크롤 게시판에 표시되는 안전보건 뉴스 한 건.
 * 테이블 NEWS_ITEMS.
 *
 * <p>고용노동부/안전보건공단/중대재해 세 갈래({@code category} 가
 * moel | kosha | accident)로 나뉘며, 설정에서 지정한 RSS/Atom 주소를
 * 주기적으로 읽어 채운다. 같은 갈래 안에서 {@code guid} 가 같으면
 * 중복으로 보고 저장하지 않는다.
 *
 * <p>{@code isDemo} 는 실제 피드를 못 가져왔을 때(사내망 차단 등)
 * 화면이 비어 보이지 않도록 채워 넣은 예시 데이터라는 표시다.
 * 실제 데이터가 들어오면 같은 갈래의 예시 항목은 정리된다.
 */
public class NewsItem {

    private Long id;
    private String category;
    private String sourceName;
    private String title;
    private String link;
    private String guid;
    private LocalDateTime publishedAt;
    private LocalDateTime fetchedAt;
    private Boolean isDemo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public Boolean getIsDemo() {
        return isDemo;
    }

    public void setIsDemo(Boolean isDemo) {
        this.isDemo = isDemo;
    }
}
