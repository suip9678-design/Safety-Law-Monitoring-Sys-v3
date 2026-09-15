package com.safetylaw.monitor.support;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * 시각 처리 기준.
 *
 * <p>DB 의 모든 TIMESTAMP 컬럼은 <b>UTC</b> 로 저장한다(기존 Python 버전과 동일).
 * 반면 스케줄 작업은 한국 시각(KST) 기준으로 돈다. 두 기준을 섞으면
 * 서버 시간대가 UTC 로 맞춰진 환경에서 몇 시간씩 어긋나므로, 저장은 항상
 * {@link #nowUtc()} 를 쓰고 스케줄 표기는 항상 {@link #KST} 를 명시한다.
 */
public final class Times {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private Times() {
    }

    /** DB 에 저장할 현재 시각(UTC). */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * DB 에서 읽은 UTC 시각을 JSON 으로 내보내기 위한 형태로 바꾼다.
     *
     * <p>이 변환을 빠뜨리면 JSON 에 시간대 표시가 없는 "2026-09-15T02:47:04"
     * 형태로 나가고, 브라우저가 이를 <b>현지 시각</b>으로 해석해 화면에
     * 9시간 어긋난 값이 표시된다. 반드시 UTC 오프셋을 붙여 내보내야 한다.
     */
    public static OffsetDateTime toUtcOffset(LocalDateTime utc) {
        return utc == null ? null : utc.atOffset(ZoneOffset.UTC);
    }
}
