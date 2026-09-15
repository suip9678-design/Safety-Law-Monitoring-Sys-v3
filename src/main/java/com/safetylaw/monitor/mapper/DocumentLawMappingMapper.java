package com.safetylaw.monitor.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.safetylaw.monitor.domain.DocumentLawMapping;
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

    void deleteById(@Param("id") Long id);
}
