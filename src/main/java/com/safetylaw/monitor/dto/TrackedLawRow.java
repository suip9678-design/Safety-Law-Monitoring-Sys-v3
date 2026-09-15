package com.safetylaw.monitor.dto;

import com.safetylaw.monitor.domain.TrackedLaw;

/**
 * 목록 화면용 조회 결과.
 *
 * <p>법령 기본 정보에 집계값 두 개를 얹은 형태다. 목록에서 법령마다 따로
 * 세면 건수만큼 쿼리가 나가므로 조회 한 번에 함께 계산해 담는다.
 */
public class TrackedLawRow extends TrackedLaw {

    private int mappedDocumentCount;
    private int unreviewedRevisionCount;

    public int getMappedDocumentCount() {
        return mappedDocumentCount;
    }

    public void setMappedDocumentCount(int mappedDocumentCount) {
        this.mappedDocumentCount = mappedDocumentCount;
    }

    public int getUnreviewedRevisionCount() {
        return unreviewedRevisionCount;
    }

    public void setUnreviewedRevisionCount(int unreviewedRevisionCount) {
        this.unreviewedRevisionCount = unreviewedRevisionCount;
    }
}
