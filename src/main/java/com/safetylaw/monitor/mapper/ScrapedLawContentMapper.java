package com.safetylaw.monitor.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.safetylaw.monitor.domain.ScrapedLawContent;

public interface ScrapedLawContentMapper {

    void insert(ScrapedLawContent content);

    /**
     * 같은 (구분, 일련번호) 행이 이미 있으면 갱신한다.
     *
     * @return 갱신된 행 수. 0 이면 아직 없는 항목이므로 호출 측에서 insert 한다.
     */
    int updateByExternalKey(ScrapedLawContent content);

    ScrapedLawContent findByExternalKey(@Param("sourceType") String sourceType,
                                        @Param("externalId") String externalId);

    /**
     * 검색어가 법령명 또는 본문에 들어 있는 행의 ID.
     *
     * <p>본문(CLOB)까지 한꺼번에 읽어오면 전체 법령 캐시를 켠 경우 수천 건
     * 분량이 되어 메모리를 크게 쓴다. 그래서 먼저 걸리는 행의 ID 만 찾고,
     * 본문은 {@link #findByIds(List)} 로 걸린 것만 읽는다.
     *
     * @param sourceType "all" 이면 구분을 가리지 않는다
     */
    List<Long> findMatchingIds(@Param("sourceType") String sourceType,
                               @Param("query") String query,
                               @Param("limit") int limit);

    /** 본문까지 포함해 읽는다. 검색에 걸린 행에만 쓸 것. */
    List<ScrapedLawContent> findByIds(@Param("ids") List<Long> ids);

    /**
     * 캐시된 항목의 식별자와 공포번호만 읽는다.
     *
     * <p>전체 법령 캐시에서 "공포번호가 그대로면 상세 조회를 건너뛴다"를
     * 판단하는 데 쓴다. 항목마다 따로 조회하지 않도록 한 번에 읽되,
     * 본문(CLOB)은 제외해야 수만 건이어도 메모리를 적게 쓴다.
     */
    List<ScrapedLawContent> findAllPromulgationKeys();

    int countAll();

    LocalDateTime findMaxCachedAt();
}
