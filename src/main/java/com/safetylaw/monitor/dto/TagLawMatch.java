package com.safetylaw.monitor.dto;

/** 키워드 하나가 어떤 법령에 걸렸는지 나타내는 짝. */
public class TagLawMatch {

    private String tag;
    private Long lawId;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Long getLawId() {
        return lawId;
    }

    public void setLawId(Long lawId) {
        this.lawId = lawId;
    }
}
