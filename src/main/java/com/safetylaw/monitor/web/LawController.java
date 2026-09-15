package com.safetylaw.monitor.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.safetylaw.monitor.dto.LawSearchResultResponse;
import com.safetylaw.monitor.dto.NewAdmrulCandidateResponse;
import com.safetylaw.monitor.dto.RevisionRequests;
import com.safetylaw.monitor.dto.TrackedLawCreateRequest;
import com.safetylaw.monitor.dto.TrackedLawResponse;
import com.safetylaw.monitor.service.LawService;
import com.safetylaw.monitor.service.SyncService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/laws")
public class LawController {

    private final LawService lawService;

    public LawController(LawService lawService) {
        this.lawService = lawService;
    }

    @GetMapping("/search")
    public List<LawSearchResultResponse> search(@RequestParam("source_type") String sourceType,
                                                @RequestParam("query") String query) {
        if (!"law".equals(sourceType) && !"admrul".equals(sourceType)) {
            throw new IllegalArgumentException("source_type은 law 또는 admrul 이어야 합니다.");
        }
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("검색어를 입력하세요.");
        }
        return lawService.search(sourceType, query);
    }

    @GetMapping
    public List<TrackedLawResponse> list(
            @RequestParam(name = "active_only", defaultValue = "true") boolean activeOnly,
            @RequestParam(name = "source_type", required = false) String sourceType) {
        return lawService.list(activeOnly, sourceType);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrackedLawResponse add(@Valid @RequestBody TrackedLawCreateRequest request) {
        return lawService.register(request);
    }

    // --- 신규 제정 고시 후보 ---
    // 아래 경로들은 /{lawId} 보다 먼저 선언해 둔다. 구체적인 경로가 먼저
    // 매칭되어야 "new-admrul-candidates"가 법령 번호로 해석되지 않는다.

    @PostMapping("/new-admrul-candidates/{candidateId}/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismissCandidate(@PathVariable Long candidateId) {
        lawService.updateCandidateStatus(candidateId, SyncService.CANDIDATE_DISMISSED);
    }

    /**
     * 무시해 둔 후보 목록.
     *
     * <p>한 번 무시하면 다음 탐지부터 다시 뜨지 않는데, 실수로 무시했을 때
     * 되돌릴 방법이 화면에 없으면 확인할 길이 없다. 이 목록과 복원 기능이
     * 그 역할을 한다.
     */
    @GetMapping("/new-admrul-candidates/dismissed")
    public List<NewAdmrulCandidateResponse> dismissedCandidates() {
        return lawService.dismissedCandidates();
    }

    @PostMapping("/new-admrul-candidates/{candidateId}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restoreCandidate(@PathVariable Long candidateId) {
        lawService.updateCandidateStatus(candidateId, SyncService.CANDIDATE_NEW);
    }

    @PostMapping("/new-admrul-candidates/bulk-restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bulkRestoreCandidates(@Valid @RequestBody RevisionRequests.BulkIds request) {
        lawService.bulkRestoreCandidates(request.ids());
    }

    /** 화면에서 후보를 등록한 직후 호출해, 신규 후보 목록에서 빠지게 한다. */
    @PostMapping("/new-admrul-candidates/{candidateId}/registered")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markCandidateRegistered(@PathVariable Long candidateId) {
        lawService.updateCandidateStatus(candidateId, SyncService.CANDIDATE_REGISTERED);
    }

    @GetMapping("/{lawId}")
    public TrackedLawResponse get(@PathVariable Long lawId) {
        return lawService.get(lawId);
    }

    /** 추적을 그만둔다. 지금까지의 개정 이력은 기록으로 남는다. */
    @DeleteMapping("/{lawId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long lawId) {
        lawService.deactivate(lawId);
    }
}
