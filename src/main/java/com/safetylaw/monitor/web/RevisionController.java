package com.safetylaw.monitor.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.safetylaw.monitor.dto.LawRevisionResponse;
import com.safetylaw.monitor.dto.RevisionRequests;
import com.safetylaw.monitor.service.RevisionService;
import com.safetylaw.monitor.service.SyncService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/revisions")
public class RevisionController {

    private final RevisionService revisionService;

    public RevisionController(RevisionService revisionService) {
        this.revisionService = revisionService;
    }

    /**
     * @param hasMappedDocuments 사규 개정 이력 탭 전용. 사내 문서와 이어진
     *                           법령의 개정만 추린다.
     */
    @GetMapping
    public List<LawRevisionResponse> list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "tracked_law_id", required = false) Long trackedLawId,
            @RequestParam(name = "has_mapped_documents", defaultValue = "false") boolean hasMappedDocuments,
            @RequestParam(name = "limit", defaultValue = "200") int limit) {
        return revisionService.list(status, trackedLawId, hasMappedDocuments, limit);
    }

    @PatchMapping("/bulk-status")
    public List<LawRevisionResponse> bulkUpdateStatus(@Valid @RequestBody RevisionRequests.BulkStatus request) {
        requireValidStatus(request.reviewStatus());
        return revisionService.bulkUpdateStatus(request.ids(), request.reviewStatus());
    }

    @PostMapping("/bulk-delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bulkDelete(@Valid @RequestBody RevisionRequests.BulkIds request) {
        revisionService.bulkDelete(request.ids());
    }

    @PatchMapping("/{revisionId}")
    public LawRevisionResponse update(@PathVariable Long revisionId,
                                      @RequestBody RevisionRequests.Update request) {
        if (request.reviewStatus() != null) {
            requireValidStatus(request.reviewStatus());
        }
        LawRevisionResponse updated = revisionService.updateReview(
                revisionId, request.reviewStatus(), request.reviewer(), request.note());
        if (updated == null) {
            throw new NotFoundException("개정 이력을 찾을 수 없습니다.");
        }
        return updated;
    }

    private void requireValidStatus(String status) {
        if (!SyncService.REVIEW_STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "review_status는 " + SyncService.REVIEW_STATUSES + " 중 하나여야 합니다.");
        }
    }
}
