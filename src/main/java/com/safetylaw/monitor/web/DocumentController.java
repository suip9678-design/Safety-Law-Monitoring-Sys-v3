package com.safetylaw.monitor.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.safetylaw.monitor.dto.CompanyDocumentRequest;
import com.safetylaw.monitor.dto.CompanyDocumentResponse;
import com.safetylaw.monitor.dto.DocumentImpactResponse;
import com.safetylaw.monitor.service.DocumentImpactService;
import com.safetylaw.monitor.service.DocumentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentImpactService impactService;

    public DocumentController(DocumentService documentService, DocumentImpactService impactService) {
        this.documentService = documentService;
        this.impactService = impactService;
    }

    @GetMapping
    public List<CompanyDocumentResponse> list(
            @RequestParam(name = "doc_type", required = false) String docType) {
        return documentService.list(docType);
    }

    /**
     * 사규 개정 이력 탭 전용.
     *
     * <p>대시보드 위젯과 달리 상태·건수 제한 없이 전체 이력을 보여준다.
     * status 를 주면 그 상태만 추린다.
     */
    @GetMapping("/impacts")
    public List<DocumentImpactResponse> impacts(
            @RequestParam(name = "status", required = false) String status) {
        List<String> statuses = (status == null || status.isBlank()) ? List.of() : List.of(status);
        return impactService.compute(statuses, 0, 0);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyDocumentResponse create(@Valid @RequestBody CompanyDocumentRequest request) {
        return documentService.create(request);
    }

    @GetMapping("/{docId}")
    public CompanyDocumentResponse get(@PathVariable Long docId) {
        return documentService.get(docId);
    }

    @PutMapping("/{docId}")
    public CompanyDocumentResponse update(@PathVariable Long docId,
                                          @Valid @RequestBody CompanyDocumentRequest request) {
        return documentService.update(docId, request);
    }

    @DeleteMapping("/{docId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long docId) {
        documentService.delete(docId);
    }
}
