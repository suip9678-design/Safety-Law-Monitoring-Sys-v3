package com.safetylaw.monitor.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.safetylaw.monitor.domain.DocumentLawMapping;
import com.safetylaw.monitor.dto.LawDocumentTitle;
import com.safetylaw.monitor.dto.MappingRow;

public interface DocumentLawMappingMapper {

    void insert(DocumentLawMapping mapping);

    MappingRow findRowById(@Param("id") Long id);

    List<MappingRow> findRows(@Param("documentId") Long documentId,
                              @Param("trackedLawId") Long trackedLawId);

    DocumentLawMapping findByDocumentAndLaw(@Param("documentId") Long documentId,
                                            @Param("trackedLawId") Long trackedLawId);

    /** 영향도 계산용. 비활성화된 법령은 제외한다. */
    List<DocumentLawMapping> findActiveByDocumentIds(@Param("documentIds") List<Long> documentIds);

    /**
     * 법령별로 매핑된 사내 문서 제목.
     *
     * <p>개정 목록에서 "이 법이 바뀌면 어떤 문서를 봐야 하는지"를 함께 보여줄
     * 때 쓴다. 개정 건마다 따로 조회하면 건수만큼 쿼리가 나가므로 한 번에 읽는다.
     */
    List<LawDocumentTitle> findDocumentTitlesByLawIds(@Param("lawIds") List<Long> lawIds);

    void deleteById(@Param("id") Long id);
}
