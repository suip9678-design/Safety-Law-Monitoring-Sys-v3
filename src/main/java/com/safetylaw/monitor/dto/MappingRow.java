package com.safetylaw.monitor.dto;

import com.safetylaw.monitor.domain.DocumentLawMapping;

/** 매핑 목록 조회 결과. 화면 표시용 문서 제목과 법령명을 조인해 담는다. */
public class MappingRow extends DocumentLawMapping {

    private String documentTitle;
    private String trackedLawName;

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public String getTrackedLawName() {
        return trackedLawName;
    }

    public void setTrackedLawName(String trackedLawName) {
        this.trackedLawName = trackedLawName;
    }
}
