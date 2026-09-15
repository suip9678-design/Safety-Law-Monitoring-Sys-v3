package com.safetylaw.monitor.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safetylaw.monitor.domain.DocumentLawMapping;
import com.safetylaw.monitor.dto.MappingCreateRequest;
import com.safetylaw.monitor.dto.MappingResponse;
import com.safetylaw.monitor.dto.MappingRow;
import com.safetylaw.monitor.mapper.CompanyDocumentMapper;
import com.safetylaw.monitor.mapper.DocumentLawMappingMapper;
import com.safetylaw.monitor.mapper.TrackedLawMapper;
import com.safetylaw.monitor.support.Times;
import com.safetylaw.monitor.web.NotFoundException;

/** 사내 문서와 법령을 잇는 매핑 관리. */
@Service
public class MappingService {

    private final DocumentLawMappingMapper mappingMapper;
    private final CompanyDocumentMapper documentMapper;
    private final TrackedLawMapper trackedLawMapper;

    public MappingService(DocumentLawMappingMapper mappingMapper,
                          CompanyDocumentMapper documentMapper,
                          TrackedLawMapper trackedLawMapper) {
        this.mappingMapper = mappingMapper;
        this.documentMapper = documentMapper;
        this.trackedLawMapper = trackedLawMapper;
    }

    /** 이미 이어져 있는 짝을 또 잇으려 할 때. */
    public static class DuplicateMappingException extends RuntimeException {
        public DuplicateMappingException(String message) {
            super(message);
        }
    }

    public List<MappingResponse> list(Long documentId, Long trackedLawId) {
        List<MappingResponse> out = new ArrayList<>();
        for (MappingRow row : mappingMapper.findRows(documentId, trackedLawId)) {
            out.add(MappingResponse.of(row));
        }
        return out;
    }

    @Transactional
    public MappingResponse create(MappingCreateRequest request) {
        if (documentMapper.findById(request.documentId()) == null) {
            throw new NotFoundException("문서를 찾을 수 없습니다.");
        }
        if (trackedLawMapper.findById(request.trackedLawId()) == null) {
            throw new NotFoundException("추적 중인 법령을 찾을 수 없습니다.");
        }
        if (mappingMapper.findByDocumentAndLaw(request.documentId(), request.trackedLawId()) != null) {
            throw new DuplicateMappingException("이미 연결된 문서와 법령입니다.");
        }

        DocumentLawMapping mapping = new DocumentLawMapping();
        mapping.setDocumentId(request.documentId());
        mapping.setTrackedLawId(request.trackedLawId());
        mapping.setNote(request.note());
        mapping.setCreatedAt(Times.nowUtc());
        mappingMapper.insert(mapping);

        return MappingResponse.of(mappingMapper.findRowById(mapping.getId()));
    }

    @Transactional
    public void delete(Long id) {
        if (mappingMapper.findRowById(id) == null) {
            throw new NotFoundException("연결 정보를 찾을 수 없습니다.");
        }
        mappingMapper.deleteById(id);
    }
}
