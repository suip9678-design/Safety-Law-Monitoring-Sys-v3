package com.safetylaw.monitor.dto;

/** 어떤 법령에 어떤 사내 문서가 매핑돼 있는지 나타내는 짝. */
public class LawDocumentTitle {

    private Long trackedLawId;
    private String documentTitle;

    public Long getTrackedLawId() {
        return trackedLawId;
    }

    public void setTrackedLawId(Long trackedLawId) {
        this.trackedLawId = trackedLawId;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }
}
