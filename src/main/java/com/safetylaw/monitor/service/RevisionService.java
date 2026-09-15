package com.safetylaw.monitor.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safetylaw.monitor.domain.LawRevision;
import com.safetylaw.monitor.dto.LawDocumentTitle;
import com.safetylaw.monitor.dto.LawRevisionResponse;
import com.safetylaw.monitor.dto.LawRevisionRow;
import com.safetylaw.monitor.mapper.DocumentLawMappingMapper;
import com.safetylaw.monitor.mapper.LawRevisionMapper;
import com.safetylaw.monitor.support.Times;

/** 개정 이력 조회와 검토 상태 변경. */
@Service
public class RevisionService {

    private final LawRevisionMapper revisionMapper;
    private final DocumentLawMappingMapper mappingMapper;

    public RevisionService(LawRevisionMapper revisionMapper, DocumentLawMappingMapper mappingMapper) {
        this.revisionMapper = revisionMapper;
        this.mappingMapper = mappingMapper;
    }

    public List<LawRevisionResponse> list(String reviewStatus, Long trackedLawId,
                                          boolean hasMappedDocuments, int limit) {
        return toResponses(revisionMapper.findRows(reviewStatus, trackedLawId, hasMappedDocuments, limit));
    }

    public List<LawRevisionResponse> pendingForDashboard(int limit) {
        return toResponses(revisionMapper.findPendingForDashboard(limit));
    }

    /**
     * 조회 결과를 화면용 형태로 바꾼다.
     *
     * <p>각 개정에 딸린 사내 문서 제목은 개정 건마다 따로 조회하지 않고
     * 관련 법령 전체에 대해 한 번에 읽어 채운다.
     */
    public List<LawRevisionResponse> toResponses(List<LawRevisionRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, List<String>> documentsByLaw = documentTitlesByLaw(
                rows.stream().map(LawRevisionRow::getTrackedLawId).distinct().toList());

        List<LawRevisionResponse> out = new ArrayList<>(rows.size());
        for (LawRevisionRow row : rows) {
            out.add(LawRevisionResponse.of(row,
                    documentsByLaw.getOrDefault(row.getTrackedLawId(), List.of()),
                    LawRevisionResponse.MATCHED_BY_MAPPING));
        }
        return out;
    }

    public Map<Long, List<String>> documentTitlesByLaw(List<Long> lawIds) {
        if (lawIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<String>> byLaw = new LinkedHashMap<>();
        for (LawDocumentTitle row : mappingMapper.findDocumentTitlesByLawIds(lawIds)) {
            byLaw.computeIfAbsent(row.getTrackedLawId(), k -> new ArrayList<>()).add(row.getDocumentTitle());
        }
        return byLaw;
    }

    public LawRevisionResponse findOne(Long id) {
        LawRevisionRow row = revisionMapper.findRowById(id);
        if (row == null) {
            return null;
        }
        return toResponses(List.of(row)).get(0);
    }

    /** 값이 null 인 항목은 바꾸지 않는다. 상태를 바꾸면 검토 시각도 함께 남긴다. */
    @Transactional
    public LawRevisionResponse updateReview(Long id, String reviewStatus, String reviewer, String note) {
        LawRevision existing = revisionMapper.findById(id);
        if (existing == null) {
            return null;
        }

        LawRevision update = new LawRevision();
        update.setId(id);
        if (reviewStatus != null) {
            update.setReviewStatus(reviewStatus);
            update.setReviewedAt(Times.nowUtc());
        }
        update.setReviewer(reviewer);
        update.setNote(note);
        revisionMapper.updateReview(update);

        return findOne(id);
    }

    @Transactional
    public List<LawRevisionResponse> bulkUpdateStatus(List<Long> ids, String reviewStatus) {
        LocalDateTime now = Times.nowUtc();
        revisionMapper.bulkUpdateStatus(ids, reviewStatus, now);
        return toResponses(revisionMapper.findByIds(ids));
    }

    @Transactional
    public void bulkDelete(List<Long> ids) {
        revisionMapper.deleteByIds(ids);
    }
}
