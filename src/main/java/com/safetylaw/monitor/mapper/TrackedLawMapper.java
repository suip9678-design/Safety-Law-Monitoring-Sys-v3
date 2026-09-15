package com.safetylaw.monitor.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.safetylaw.monitor.domain.TrackedLaw;
import com.safetylaw.monitor.dto.TrackedLawRow;

public interface TrackedLawMapper {

    void insert(TrackedLaw law);

    TrackedLaw findById(@Param("id") Long id);

    TrackedLaw findBySourceAndExternalId(@Param("sourceType") String sourceType,
                                         @Param("externalId") String externalId);

    /** 동기화 대상 전체(활성 상태인 법령·고시). */
    List<TrackedLaw> findActive();

    /**
     * 활성 상태로 추적 중인 특정 구분의 항목들.
     *
     * <p>신규 제정 고시 탐지에서 "이미 추적 중이라 후보에서 빼야 하는지"를
     * 판단하는 기준으로 쓴다. 이 판단 기준은 스캔 시점과 유령 후보 정리
     * 시점에서 반드시 같아야 한다. 예전 Python 버전에서 한쪽에 활성 조건이
     * 빠져, 삭제한 법령이 영원히 "추적 중"으로 남아 다시는 탐지되지 않는
     * 문제가 있었다.
     */
    List<TrackedLaw> findActiveBySourceType(@Param("sourceType") String sourceType);

    /**
     * 목록 화면용 조회. 매핑된 문서 수와 미검토 개정 건수를 함께 집계한다.
     * 목록마다 건별로 다시 세면 법령 수만큼 쿼리가 나가므로 한 번에 계산한다.
     */
    List<TrackedLawRow> findAllWithCounts(@Param("sourceType") String sourceType,
                                          @Param("activeOnly") boolean activeOnly);

    TrackedLawRow findRowById(@Param("id") Long id);

    /** 동기화로 확인된 최신 공포 정보와 마지막 확인 시각을 갱신한다. */
    void updateCurrentState(TrackedLaw law);

    void deleteById(@Param("id") Long id);

    int countActive();

    /** 어떤 사내 문서와도 매핑되지 않은 법령 수(대시보드 표시용). */
    int countUnmapped();
}
