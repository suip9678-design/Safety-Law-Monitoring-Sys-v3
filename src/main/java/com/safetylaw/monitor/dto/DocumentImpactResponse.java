package com.safetylaw.monitor.dto;

import java.util.List;

/**
 * 문서 기준으로 재구성한 검토 필요 목록.
 *
 * <p>"어떤 법이 바뀌었는가"가 아니라 "어떤 절차서·지침서를 검토해야 하는가"를
 * 축으로 보여주기 위한 형태다.
 */
public record DocumentImpactResponse(
        Long documentId,
        String documentTitle,
        String docType,
        List<LawRevisionResponse> revisions) {
}
