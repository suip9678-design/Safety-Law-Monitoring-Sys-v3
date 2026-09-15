package com.safetylaw.monitor.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.safetylaw.monitor.domain.NewAdmrulCandidate;

public interface NewAdmrulCandidateMapper {

    void insert(NewAdmrulCandidate candidate);

    NewAdmrulCandidate findById(@Param("id") Long id);

    List<NewAdmrulCandidate> findByStatus(@Param("status") String status,
                                          @Param("limit") int limit);

    /**
     * 상태와 무관한 전체 후보. 다시 만들지 말아야 할 항목을 가려내는 기준으로 쓴다.
     *
     * <p>무시한 항목도 행이 남아 있어야 다음 스캔에서 같은 항목이 다시
     * 후보로 떠오르지 않는다.
     */
    List<NewAdmrulCandidate> findAll();

    void updateStatus(@Param("id") Long id, @Param("status") String status);

    void bulkUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);

    void deleteByIds(@Param("ids") List<Long> ids);
}
