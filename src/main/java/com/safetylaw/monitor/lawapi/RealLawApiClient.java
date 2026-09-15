package com.safetylaw.monitor.lawapi;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.safetylaw.monitor.support.Xml;
import com.safetylaw.monitor.support.XmlParseException;

import org.w3c.dom.Element;

/**
 * law.go.kr 공동활용 API 를 실제로 호출하는 구현. API 인증키(OC)가 필요하다.
 *
 * <p>주의: 아래 태그 이름들은 API 의 공개된 구조를 따른 것이지만, 이 코드를
 * 옮겨 쓴 원본(Python) 작성 시점에 실제 응답으로 검증되지 못했다. 그래서
 * 후보 이름을 여러 개 두고 차례로 시도하며, 상세 조회에서는 트리 전체를
 * 훑는 방식으로 태그명이 조금 달라도 값을 찾을 수 있게 했다.
 *
 * <p>인증키를 발급받은 뒤 어떤 항목이 계속 비어 있다면, 실제 응답 XML 을
 * 한 번 확인해 아래 후보 목록에 태그명을 추가하면 된다.
 */
class RealLawApiClient implements LawApiClient {

    private static final String SEARCH_PATH = "/DRF/lawSearch.do";
    private static final String SERVICE_PATH = "/DRF/lawService.do";
    private static final String SITE_BASE = "https://www.law.go.kr";

    /** 목록 조회 응답에서 한 건을 감싸는 태그. */
    private static final Map<String, String> LIST_ITEM_TAG = Map.of(
            LAW, "law",
            ADMRUL, "admrul");

    private static final Map<String, List<String>> EXTERNAL_ID = Map.of(
            LAW, List.of("법령일련번호", "MST"),
            ADMRUL, List.of("행정규칙일련번호", "ID"));

    /** 개정으로 일련번호가 바뀌어도 고정되는 영구 식별자. 개정 감지의 기준이다. */
    private static final Map<String, List<String>> MASTER_ID = Map.of(
            LAW, List.of("법령ID"),
            ADMRUL, List.of("행정규칙ID"));

    private static final Map<String, List<String>> NAME = Map.of(
            LAW, List.of("법령명한글", "법령명_한글", "법령명"),
            ADMRUL, List.of("행정규칙명"));

    private static final Map<String, List<String>> CATEGORY = Map.of(
            LAW, List.of("법령구분명", "법종구분"),
            ADMRUL, List.of("행정규칙종류명", "행정규칙종류"));

    private static final Map<String, List<String>> DEPARTMENT = Map.of(
            LAW, List.of("소관부처명", "소관부처"),
            ADMRUL, List.of("소관부처명", "소관부처"));

    private static final Map<String, List<String>> PROMULGATION_NO = Map.of(
            LAW, List.of("공포번호"),
            ADMRUL, List.of("발령번호"));

    private static final Map<String, List<String>> PROMULGATION_DATE = Map.of(
            LAW, List.of("공포일자"),
            ADMRUL, List.of("발령일자"));

    private static final Map<String, List<String>> ENFORCEMENT_DATE = Map.of(
            LAW, List.of("시행일자"),
            ADMRUL, List.of("시행일자"));

    private static final Map<String, List<String>> DETAIL_LINK = Map.of(
            LAW, List.of("법령상세링크"),
            ADMRUL, List.of("행정규칙상세링크"));

    /**
     * 조문 본문을 모을 때 볼 태그들.
     *
     * <p>상세 XML 은 조문 단위로 "조문내용"(조 전체 텍스트) 아래에 항/호/목이
     * 다시 쪼개져 나오는 경우가 있어 그것들도 함께 모은다.
     */
    private static final Set<String> CONTENT_TAGS =
            Set.of("조문내용", "항내용", "호내용", "목내용");

    private static final String ARTICLE_UNIT_TAG = "조문단위";
    private static final String ARTICLE_NO_TAG = "조문번호";
    private static final String ARTICLE_SUB_NO_TAG = "조문가지번호";

    private final RestClient restClient;
    private final String oc;

    RealLawApiClient(RestClient restClient, String oc) {
        this.restClient = restClient;
        this.oc = oc;
    }

    @Override
    public List<LawApiItem> search(String sourceType, String query, int display, int page) {
        byte[] body;
        try {
            body = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(SEARCH_PATH)
                            .queryParam("OC", oc)
                            .queryParam("target", sourceType)
                            .queryParam("type", "XML")
                            .queryParam("query", query)
                            .queryParam("display", display)
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientException e) {
            throw new LawApiException("법령 검색 API 호출에 실패했습니다: " + e.getMessage(), e);
        }
        if (body == null) {
            return List.of();
        }

        Element root = parseOrFail(body);
        String itemTag = LIST_ITEM_TAG.getOrDefault(sourceType, sourceType);

        List<LawApiItem> items = new ArrayList<>();
        for (Element element : Xml.children(root, itemTag)) {
            items.add(extract(element, sourceType, false));
        }
        return items;
    }

    @Override
    public Integer countTotal(String sourceType, String query) {
        try {
            byte[] body = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(SEARCH_PATH)
                            .queryParam("OC", oc)
                            .queryParam("target", sourceType)
                            .queryParam("type", "XML")
                            .queryParam("query", query)
                            .queryParam("display", 1)
                            .build())
                    .retrieve()
                    .body(byte[].class);
            if (body == null) {
                return null;
            }
            Element root = parseOrFail(body);
            String total = Xml.childText(root, List.of("totalCnt"));
            return (total != null && total.matches("\\d+")) ? Integer.valueOf(total) : null;
        } catch (RestClientException | LawApiException e) {
            // 진행률 표시용 부가 정보라 실패해도 기능을 막지 않는다.
            return null;
        }
    }

    @Override
    public LawApiItem getDetail(String sourceType, String externalId, String efYd) {
        String idParam = LAW.equals(sourceType) ? "MST" : "ID";
        byte[] body;
        try {
            body = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(SERVICE_PATH)
                                .queryParam("OC", oc)
                                .queryParam("target", sourceType)
                                .queryParam("type", "XML")
                                .queryParam(idParam, externalId);
                        if (LAW.equals(sourceType)) {
                            // target=law 상세 조회는 mobileYn 이 없으면 404 가 난다.
                            // efYd 는 특정 시행일자 시점을 지정한다.
                            uriBuilder.queryParam("mobileYn", "");
                            if (efYd != null && !efYd.isBlank()) {
                                uriBuilder.queryParam("efYd", efYd);
                            }
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientException e) {
            throw new LawApiException("법령 상세 API 호출에 실패했습니다: " + e.getMessage(), e);
        }
        if (body == null) {
            return null;
        }

        String head = new String(body, 0, Math.min(200, body.length), StandardCharsets.ISO_8859_1);
        if (head.contains("<!DOCTYPE html")) {
            throw new LawApiException(
                    "법령 상세 API가 오류 페이지를 반환했습니다 (파라미터 또는 일련번호 값을 확인하세요).");
        }

        Element root = parseOrFail(body);
        LawApiItem item = extract(root, sourceType, true);
        if (item.getName() == null) {
            return null;
        }
        item.setExternalId(externalId);

        FullText fullText = extractFullText(root);
        item.setContent(fullText.content());
        item.setArticleContentLen(fullText.articleLen());
        return item;
    }

    /** 해석 실패를 이 API 의 오류로 바꿔 전달한다. 호출 측이 원인을 화면에 보여줄 수 있어야 한다. */
    private static Element parseOrFail(byte[] body) {
        try {
            return Xml.parse(body).getDocumentElement();
        } catch (XmlParseException e) {
            throw new LawApiException("법령 API 응답을 해석할 수 없습니다: " + e.getMessage(), e);
        }
    }

    private LawApiItem extract(Element element, String sourceType, boolean deep) {
        LawApiItem item = new LawApiItem();
        item.setSourceType(sourceType);
        item.setExternalId(value(element, EXTERNAL_ID, sourceType, deep));
        item.setMasterId(value(element, MASTER_ID, sourceType, deep));
        item.setName(value(element, NAME, sourceType, deep));
        item.setCategory(value(element, CATEGORY, sourceType, deep));
        item.setDepartment(value(element, DEPARTMENT, sourceType, deep));
        item.setPromulgationNo(value(element, PROMULGATION_NO, sourceType, deep));
        item.setPromulgationDate(value(element, PROMULGATION_DATE, sourceType, deep));
        item.setEnforcementDate(value(element, ENFORCEMENT_DATE, sourceType, deep));
        item.setDetailLink(normalizeLink(value(element, DETAIL_LINK, sourceType, deep)));
        return item;
    }

    private String value(Element element, Map<String, List<String>> candidates,
                         String sourceType, boolean deep) {
        List<String> tags = candidates.getOrDefault(sourceType, List.of());
        if (tags.isEmpty()) {
            return null;
        }
        return deep ? Xml.textAnywhere(element, tags) : Xml.childText(element, tags);
    }

    private static String normalizeLink(String link) {
        if (link == null) {
            return null;
        }
        if (link.startsWith("http://") || link.startsWith("https://")) {
            return link;
        }
        return SITE_BASE + (link.startsWith("/") ? link : "/" + link);
    }

    /**
     * 조문 본문을 모은 결과.
     *
     * @param articleLen 앞부분 중 조번호 라벨이 붙었을 수 있는 구간의 길이.
     *                   검색어가 이 구간 안에서 걸렸을 때만 조문 링크를 만든다.
     *                   그래야 별표처럼 조번호를 모르는 뒷부분 내용이 엉뚱하게
     *                   직전 조문 링크로 이어지지 않는다.
     */
    private record FullText(String content, int articleLen) {
    }

    /**
     * 조문(본칙)은 조문단위별로 조번호를 앞에 붙여 모으고, 별표·서식·부칙 등
     * 조문단위 밖의 본문도 빠짐없이 이어 붙인다.
     *
     * <p>조문 링크를 만들려고 조번호를 붙이더라도 본문 캐시가 담는 범위는
     * 줄어들면 안 된다. 원본에서 조문단위 밖 텍스트를 통째로 빠뜨려 별표
     * 내용이 캐시에서 사라졌던 적이 있다.
     */
    private FullText extractFullText(Element root) {
        List<String> parts = new ArrayList<>();
        Set<Element> covered = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Element unit : Xml.descendantsAndSelf(root, List.of(ARTICLE_UNIT_TAG))) {
            List<String> texts = new ArrayList<>();
            for (Element el : Xml.descendantsAndSelf(unit, CONTENT_TAGS)) {
                String text = Xml.trimmed(el.getTextContent());
                if (text != null) {
                    texts.add(text);
                    covered.add(el);
                }
            }
            String block = String.join(" ", texts).trim();
            if (block.isEmpty()) {
                continue;
            }
            String label = articleLabel(unit);
            // 조문내용에 이미 "제N조" 문구가 들어 있으면 덧붙이지 않는다.
            if (label != null && !block.substring(0, Math.min(20, block.length())).contains(label)) {
                block = label + " " + block;
            }
            parts.add(block);
        }

        int articleLen = String.join("\n", parts).length();

        for (Element el : Xml.descendantsAndSelf(root, CONTENT_TAGS)) {
            if (covered.contains(el)) {
                continue;
            }
            String text = Xml.trimmed(el.getTextContent());
            if (text != null) {
                parts.add(text);
            }
        }

        return new FullText(String.join("\n", parts), articleLen);
    }

    private static String articleLabel(Element unit) {
        String no = Xml.childText(unit, List.of(ARTICLE_NO_TAG));
        if (no == null || !no.matches("\\d+")) {
            return null;
        }
        StringBuilder label = new StringBuilder("제").append(Integer.parseInt(no)).append("조");
        String subNo = Xml.childText(unit, List.of(ARTICLE_SUB_NO_TAG));
        if (subNo != null && subNo.matches("\\d+") && Integer.parseInt(subNo) > 0) {
            label.append("의").append(Integer.parseInt(subNo));
        }
        return label.toString();
    }
}
