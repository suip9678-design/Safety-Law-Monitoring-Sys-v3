package com.safetylaw.monitor.dto;

/** 문서 하나에 매핑된 법령 이름 한 건. */
public class DocumentLawName {

    private Long documentId;
    private String lawName;

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getLawName() {
        return lawName;
    }

    public void setLawName(String lawName) {
        this.lawName = lawName;
    }
}
