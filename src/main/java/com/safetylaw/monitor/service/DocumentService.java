package com.safetylaw.monitor.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safetylaw.monitor.domain.CompanyDocument;
import com.safetylaw.monitor.dto.CompanyDocumentRequest;
import com.safetylaw.monitor.dto.CompanyDocumentResponse;
import com.safetylaw.monitor.dto.DocumentLawName;
import com.safetylaw.monitor.mapper.CompanyDocumentMapper;
import com.safetylaw.monitor.support.Times;
import com.safetylaw.monitor.web.NotFoundException;

/** 사내 절차서·지침서·작업표준 관리. */
@Service
public class DocumentService {

    public static final List<String> DOC_TYPES = List.of("절차서", "지침서", "작업표준", "기타");

    private final CompanyDocumentMapper documentMapper;

    public DocumentService(CompanyDocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    public List<CompanyDocumentResponse> list(String docType) {
        List<CompanyDocument> documents = documentMapper.findAll(docType);
        if (documents.isEmpty()) {
            return List.of();
        }
        Map<Long, List<String>> lawsByDocument = mappedLawNames(
                documents.stream().map(CompanyDocument::getId).toList());

        List<CompanyDocumentResponse> out = new ArrayList<>(documents.size());
        for (CompanyDocument document : documents) {
            out.add(CompanyDocumentResponse.of(document,
                    lawsByDocument.getOrDefault(document.getId(), List.of())));
        }
        return out;
    }

    public CompanyDocumentResponse get(Long id) {
        CompanyDocument document = documentMapper.findById(id);
        if (document == null) {
            throw new NotFoundException("문서를 찾을 수 없습니다.");
        }
        return CompanyDocumentResponse.of(document,
                mappedLawNames(List.of(id)).getOrDefault(id, List.of()));
    }

    @Transactional
    public CompanyDocumentResponse create(CompanyDocumentRequest request) {
        requireValidDocType(request.docTypeOrDefault());

        CompanyDocument document = new CompanyDocument();
        apply(document, request);
        document.setCreatedAt(Times.nowUtc());
        document.setUpdatedAt(Times.nowUtc());
        documentMapper.insert(document);

        return CompanyDocumentResponse.of(document, List.of());
    }

    @Transactional
    public CompanyDocumentResponse update(Long id, CompanyDocumentRequest request) {
        CompanyDocument document = documentMapper.findById(id);
        if (document == null) {
            throw new NotFoundException("문서를 찾을 수 없습니다.");
        }
        requireValidDocType(request.docTypeOrDefault());

        apply(document, request);
        document.setUpdatedAt(Times.nowUtc());
        documentMapper.update(document);

        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        if (documentMapper.findById(id) == null) {
            throw new NotFoundException("문서를 찾을 수 없습니다.");
        }
        documentMapper.deleteById(id);
    }

    private Map<Long, List<String>> mappedLawNames(List<Long> documentIds) {
        if (documentIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<String>> byDocument = new LinkedHashMap<>();
        for (DocumentLawName row : documentMapper.findMappedLawNames(documentIds)) {
            byDocument.computeIfAbsent(row.getDocumentId(), k -> new ArrayList<>()).add(row.getLawName());
        }
        return byDocument;
    }

    private void apply(CompanyDocument document, CompanyDocumentRequest request) {
        document.setDocType(request.docTypeOrDefault());
        document.setDocNumber(request.docNumber());
        document.setTitle(request.title());
        document.setRevisionNo(request.revisionNo());
        document.setRevisionDate(request.revisionDate());
        document.setOwner(request.owner());
        document.setFileLink(request.fileLink());
        document.setNote(request.note());
        document.setTags(request.tags());
    }

    private void requireValidDocType(String docType) {
        if (!DOC_TYPES.contains(docType)) {
            throw new IllegalArgumentException("doc_type은 " + DOC_TYPES + " 중 하나여야 합니다.");
        }
    }
}
