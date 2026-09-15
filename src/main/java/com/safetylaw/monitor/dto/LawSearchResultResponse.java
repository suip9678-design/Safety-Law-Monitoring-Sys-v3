package com.safetylaw.monitor.dto;

import com.safetylaw.monitor.lawapi.LawApiItem;

/** 법령 검색 결과 한 건. */
public record LawSearchResultResponse(
        String sourceType,
        String externalId,
        String masterId,
        String name,
        String category,
        String department,
        String promulgationNo,
        String promulgationDate,
        String enforcementDate,
        String detailLink) {

    public static LawSearchResultResponse of(LawApiItem item) {
        return new LawSearchResultResponse(
                item.getSourceType(),
                item.getExternalId(),
                item.getMasterId(),
                item.getName(),
                item.getCategory(),
                item.getDepartment(),
                item.getPromulgationNo(),
                item.getPromulgationDate(),
                item.getEnforcementDate(),
                item.getDetailLink());
    }
}
