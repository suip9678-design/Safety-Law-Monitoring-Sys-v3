package com.safetylaw.monitor.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.safetylaw.monitor.domain.CompanyDocument;
import com.safetylaw.monitor.dto.DocumentLawName;
import com.safetylaw.monitor.dto.TagLawMatch;

public interface CompanyDocumentMapper {

    void insert(CompanyDocument document);

    CompanyDocument findById(@Param("id") Long id);

    List<CompanyDocument> findAll(@Param("docType") String docType);

    List<CompanyDocument> findByIds(@Param("ids") List<Long> ids);

    void update(CompanyDocument document);

    void deleteById(@Param("id") Long id);

    int countAll();

    /** 문서별로 매핑된 법령 이름. 목록 화면에서 문서마다 다시 조회하지 않도록 한 번에 가져온다. */
    List<DocumentLawName> findMappedLawNames(@Param("documentIds") List<Long> documentIds);

    /**
     * 법령과 매핑돼 있거나 키워드가 등록된 문서의 ID.
     *
     * <p>둘 중 어느 쪽도 없는 문서는 법령 개정의 영향을 판단할 근거가 없어
     * 영향도 계산 대상에서 빠진다.
     */
    List<Long> findIdsWithMappingsOrTags();

    /**
     * 주어진 키워드가 법령명 또는 법령 본문 캐시에 들어 있는 (키워드, 법령ID) 짝.
     *
     * <p>기존 Python 버전은 본문 캐시를 통째로 메모리에 올려 비교했다.
     * 전체 법령 캐시를 켜면 수천 건이 되어 메모리를 크게 쓰므로, 여기서는
     * 검색을 DB 쪽에서 수행한다. 본문은 CLOB 이라 일반 INSTR 대신
     * DBMS_LOB.INSTR 을 쓴다.
     *
     * <p>키워드 전체를 한 번의 조회로 처리한다. 문서마다 따로 조회하면
     * 문서 수만큼 쿼리가 나간다.
     */
    List<TagLawMatch> findLawIdsByTags(@Param("tags") List<String> tags);
}
