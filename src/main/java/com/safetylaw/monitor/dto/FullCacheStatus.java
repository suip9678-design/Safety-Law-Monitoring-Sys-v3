package com.safetylaw.monitor.dto;

/**
 * 전체 법령 본문 캐시 작업의 진행 상황.
 *
 * @param total 진행률 표시용 총 건수. 확인하지 못했으면 null 이며,
 *              이 경우 화면은 진행 건수만 보여준다
 */
public record FullCacheStatus(
        boolean running,
        int processed,
        int skipped,
        Integer total,
        String startedAt,
        String finishedAt,
        String error) {

    public static FullCacheStatus idle() {
        return new FullCacheStatus(false, 0, 0, null, null, null, null);
    }
}
