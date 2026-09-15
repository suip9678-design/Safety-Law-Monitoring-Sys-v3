package com.safetylaw.monitor.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.safetylaw.monitor.domain.CompanyDocument;
import com.safetylaw.monitor.domain.DocumentLawMapping;
import com.safetylaw.monitor.dto.DocumentImpactResponse;
import com.safetylaw.monitor.dto.LawRevisionResponse;
import com.safetylaw.monitor.dto.LawRevisionRow;
import com.safetylaw.monitor.dto.TagLawMatch;
import com.safetylaw.monitor.mapper.CompanyDocumentMapper;
import com.safetylaw.monitor.mapper.DocumentLawMappingMapper;
import com.safetylaw.monitor.mapper.LawRevisionMapper;

/**
 * "어떤 절차서·지침서를 검토해야 하는가"를 축으로 개정 이력을 재구성한다.
 *
 * <p>문서가 목록에 포함되는 경로는 두 가지다.
 * <ul>
 *   <li>mapping - 사용자가 문서와 법령을 직접 이어 둔 경우</li>
 *   <li>tag - 문서에 등록한 키워드가, 이어 두지 않은 법령이라도 그 법령명이나
 *       본문에 들어 있는 경우. 예를 들어 "밀폐공간"을 키워드로 등록해 두면
 *       따로 매핑하지 않아도 그 말이 들어간 법령이 개정될 때만 이 문서가 뜬다.</li>
 * </ul>
 */
@Service
public class DocumentImpactService {

    private final CompanyDocumentMapper documentMapper;
    private final DocumentLawMappingMapper mappingMapper;
    private final LawRevisionMapper revisionMapper;
    private final RevisionService revisionService;

    public DocumentImpactService(CompanyDocumentMapper documentMapper,
                                 DocumentLawMappingMapper mappingMapper,
                                 LawRevisionMapper revisionMapper,
                                 RevisionService revisionService) {
        this.documentMapper = documentMapper;
        this.mappingMapper = mappingMapper;
        this.revisionMapper = revisionMapper;
        this.revisionService = revisionService;
    }

    /**
     * @param statuses             비어 있으면 상태를 가리지 않는다(전체 이력 화면).
     *                             대시보드 위젯은 아직 처리되지 않은 상태만 넘긴다.
     * @param limitDocuments       0 이하면 제한 없음
     * @param limitRevisionsPerDoc 0 이하면 제한 없음
     */
    public List<DocumentImpactResponse> compute(List<String> statuses,
                                                int limitDocuments,
                                                int limitRevisionsPerDoc) {
        List<Long> documentIds = documentMapper.findIdsWithMappingsOrTags();
        if (documentIds.isEmpty()) {
            return List.of();
        }

        List<CompanyDocument> documents = documentMapper.findByIds(documentIds);

        // 직접 매핑해 둔 법령 (비활성화된 법령은 조회 단계에서 빠진다)
        Map<Long, Set<Long>> mappedLawsByDocument = new HashMap<>();
        for (DocumentLawMapping mapping : mappingMapper.findActiveByDocumentIds(documentIds)) {
            mappedLawsByDocument
                    .computeIfAbsent(mapping.getDocumentId(), k -> new LinkedHashSet<>())
                    .add(mapping.getTrackedLawId());
        }

        // 키워드로 걸리는 법령. 문서마다 조회하면 문서 수만큼 쿼리가 나가므로
        // 등록된 키워드 전체를 한 번에 맞춰 본다.
        Map<String, Set<Long>> lawsByTag = lawsByTag(documents);

        Map<Long, Map<Long, String>> matchedByDocument = new LinkedHashMap<>();
        Set<Long> allLawIds = new LinkedHashSet<>();

        for (CompanyDocument document : documents) {
            Map<Long, String> matched = new LinkedHashMap<>();
            for (Long lawId : mappedLawsByDocument.getOrDefault(document.getId(), Set.of())) {
                matched.put(lawId, LawRevisionResponse.MATCHED_BY_MAPPING);
            }
            for (String tag : parseTags(document.getTags())) {
                for (Long lawId : lawsByTag.getOrDefault(tag, Set.of())) {
                    matched.putIfAbsent(lawId, LawRevisionResponse.MATCHED_BY_TAG);
                }
            }
            if (!matched.isEmpty()) {
                matchedByDocument.put(document.getId(), matched);
                allLawIds.addAll(matched.keySet());
            }
        }

        if (allLawIds.isEmpty()) {
            return List.of();
        }

        Map<Long, List<LawRevisionRow>> revisionsByLaw = new HashMap<>();
        for (LawRevisionRow row : revisionMapper.findByLawIdsAndStatuses(List.copyOf(allLawIds), statuses)) {
            revisionsByLaw.computeIfAbsent(row.getTrackedLawId(), k -> new ArrayList<>()).add(row);
        }
        Map<Long, List<String>> documentTitlesByLaw = revisionService.documentTitlesByLaw(List.copyOf(allLawIds));

        Map<Long, CompanyDocument> documentsById = new HashMap<>();
        documents.forEach(d -> documentsById.put(d.getId(), d));

        List<DocumentImpactResponse> impacts = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, String>> entry : matchedByDocument.entrySet()) {
            CompanyDocument document = documentsById.get(entry.getKey());
            if (document == null) {
                continue;
            }

            record Pair(LawRevisionRow row, String matchedBy) {
            }
            List<Pair> pairs = new ArrayList<>();
            for (Map.Entry<Long, String> matched : entry.getValue().entrySet()) {
                for (LawRevisionRow row : revisionsByLaw.getOrDefault(matched.getKey(), List.of())) {
                    pairs.add(new Pair(row, matched.getValue()));
                }
            }
            if (pairs.isEmpty()) {
                continue;
            }

            pairs.sort(Comparator.comparing((Pair p) -> p.row().getDetectedAt(),
                    Comparator.nullsLast(Comparator.reverseOrder())));
            if (limitRevisionsPerDoc > 0 && pairs.size() > limitRevisionsPerDoc) {
                pairs = pairs.subList(0, limitRevisionsPerDoc);
            }

            List<LawRevisionResponse> revisions = new ArrayList<>(pairs.size());
            for (Pair pair : pairs) {
                revisions.add(LawRevisionResponse.of(pair.row(),
                        documentTitlesByLaw.getOrDefault(pair.row().getTrackedLawId(), List.of()),
                        pair.matchedBy()));
            }

            impacts.add(new DocumentImpactResponse(
                    document.getId(), document.getTitle(), document.getDocType(), revisions));
        }

        impacts.sort(Comparator.comparing(
                (DocumentImpactResponse impact) -> impact.revisions().get(0).detectedAt(),
                Comparator.nullsLast(Comparator.reverseOrder())));

        if (limitDocuments > 0 && impacts.size() > limitDocuments) {
            return impacts.subList(0, limitDocuments);
        }
        return impacts;
    }

    private Map<String, Set<Long>> lawsByTag(List<CompanyDocument> documents) {
        Set<String> allTags = new LinkedHashSet<>();
        for (CompanyDocument document : documents) {
            allTags.addAll(parseTags(document.getTags()));
        }
        if (allTags.isEmpty()) {
            return Map.of();
        }

        Map<String, Set<Long>> byTag = new HashMap<>();
        for (TagLawMatch match : documentMapper.findLawIdsByTags(List.copyOf(allTags))) {
            byTag.computeIfAbsent(match.getTag(), k -> new LinkedHashSet<>()).add(match.getLawId());
        }
        return byTag;
    }

    /** 쉼표로 구분된 키워드 목록을 쪼갠다. 저장할 때 '#' 는 빼고 넣는다. */
    private List<String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                tags.add(trimmed);
            }
        }
        return tags;
    }
}
