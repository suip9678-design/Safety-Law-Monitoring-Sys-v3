package com.safetylaw.monitor.dto;

/**
 * 키워드 검색 결과 한 건.
 *
 * @param matchedIn   검색어가 법령명에서 걸렸는지("name") 본문에서 걸렸는지("content")
 * @param snippet     본문에서 걸린 경우 검색어 주변 발췌문
 * @param articleLink 그 조문으로 바로 이동하는 링크. 조번호를 찾지 못하면 null
 */
public record KeywordSearchResult(
        String sourceType,
        String externalId,
        String masterId,
        String name,
        String category,
        String department,
        String promulgationNo,
        String promulgationDate,
        String enforcementDate,
        String detailLink,
        String matchedIn,
        String snippet,
        String articleLink) {
}
