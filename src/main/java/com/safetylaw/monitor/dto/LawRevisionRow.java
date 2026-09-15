package com.safetylaw.monitor.dto;

import com.safetylaw.monitor.domain.LawRevision;

/**
 * 개정 이력 조회 결과. 화면에 함께 보여줄 법령명/구분을 조인해 담는다.
 *
 * <p>매핑된 사내 문서 제목({@code mappedDocuments})은 이 조회에 포함하지
 * 않는다. 한 법령에 여러 문서가 걸릴 수 있어 조인하면 행이 불어나기
 * 때문이다. 서비스 계층에서 한 번에 따로 조회해 채운다.
 */
public class LawRevisionRow extends LawRevision {

    private String trackedLawName;
    private String trackedLawCategory;

    public String getTrackedLawName() {
        return trackedLawName;
    }

    public void setTrackedLawName(String trackedLawName) {
        this.trackedLawName = trackedLawName;
    }

    public String getTrackedLawCategory() {
        return trackedLawCategory;
    }

    public void setTrackedLawCategory(String trackedLawCategory) {
        this.trackedLawCategory = trackedLawCategory;
    }
}
