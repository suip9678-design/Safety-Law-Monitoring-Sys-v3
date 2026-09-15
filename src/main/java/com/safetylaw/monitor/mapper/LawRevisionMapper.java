package com.safetylaw.monitor.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.safetylaw.monitor.domain.LawRevision;
import com.safetylaw.monitor.dto.LawRevisionRow;
import com.safetylaw.monitor.dto.StatusCount;

public interface LawRevisionMapper {

    void insert(LawRevision revision);

    LawRevision findById(@Param("id") Long id);

    LawRevisionRow findRowById(@Param("id") Long id);

    /**
     * 개정 이력 목록.
     *
     * @param hasMappedDocuments 사내 문서와 매핑된 법령의 개정만 추릴지 여부
     *                           ("사규 개정 이력" 탭 전용)
     */
    List<LawRevisionRow> findRows(@Param("reviewStatus") String reviewStatus,
                                  @Param("trackedLawId") Long trackedLawId,
                                  @Param("hasMappedDocuments") boolean hasMappedDocuments,
                                  @Param("limit") int limit);

    List<LawRevisionRow> findByIds(@Param("ids") List<Long> ids);

    /**
     * 대시보드에 띄울 "아직 확인이 필요한" 개정.
     *
     * <p>비활성화한 법령의 이력과, 이미 반영완료/해당없음으로 처리한 건은
     * 제외한다. 처리 이력 전체는 개정 이력 탭에서 계속 볼 수 있다.
     */
    List<LawRevisionRow> findPendingForDashboard(@Param("limit") int limit);

    /** 활성 법령에 한정한 검토 상태별 건수. */
    List<StatusCount> countByStatusForActiveLaws();

    void updateReview(LawRevision revision);

    void bulkUpdateStatus(@Param("ids") List<Long> ids,
                          @Param("reviewStatus") String reviewStatus,
                          @Param("reviewedAt") LocalDateTime reviewedAt);

    void deleteByIds(@Param("ids") List<Long> ids);
}
