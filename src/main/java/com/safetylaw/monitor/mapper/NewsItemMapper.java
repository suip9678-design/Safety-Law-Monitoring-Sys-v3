package com.safetylaw.monitor.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.safetylaw.monitor.domain.NewsItem;

public interface NewsItemMapper {

    void insert(NewsItem item);

    /** 같은 갈래 안에서 같은 guid 가 이미 있는지. 중복 저장을 막는다. */
    int countByCategoryAndGuid(@Param("category") String category,
                               @Param("guid") String guid);

    List<NewsItem> findByCategory(@Param("category") String category,
                                  @Param("limit") int limit);

    List<NewsItem> findRecent(@Param("limit") int limit);

    /** 실제 데이터가 들어온 갈래의 예시 항목을 정리한다. */
    void deleteDemoByCategory(@Param("category") String category);

    /** 예시가 아닌 실제 항목 수. 예시 항목을 지워도 되는지 판단하는 데 쓴다. */
    int countRealByCategory(@Param("category") String category);
}
