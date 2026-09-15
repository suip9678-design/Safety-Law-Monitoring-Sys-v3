package com.safetylaw.monitor.dto;

import java.time.OffsetDateTime;

import com.safetylaw.monitor.domain.NewsItem;
import com.safetylaw.monitor.support.Times;

/** 화면에 내보내는 뉴스 한 건. 중복 판단용 guid 는 내보내지 않는다. */
public record NewsItemResponse(
        Long id,
        String category,
        String sourceName,
        String title,
        String link,
        OffsetDateTime publishedAt,
        OffsetDateTime fetchedAt,
        Boolean isDemo) {

    public static NewsItemResponse of(NewsItem item) {
        return new NewsItemResponse(
                item.getId(),
                item.getCategory(),
                item.getSourceName(),
                item.getTitle(),
                item.getLink(),
                Times.toUtcOffset(item.getPublishedAt()),
                Times.toUtcOffset(item.getFetchedAt()),
                item.getIsDemo());
    }
}
