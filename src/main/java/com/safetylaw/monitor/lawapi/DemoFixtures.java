package com.safetylaw.monitor.lawapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * API 인증키가 없을 때 쓰는 예시 데이터.
 *
 * <p>인증키를 발급받기 전에도 검색·등록·문서 매핑 같은 흐름을 그대로
 * 확인할 수 있게 한다. 값은 설명을 위한 예시일 뿐 실제 현행 법령 내용과
 * 일치한다고 보장하지 않는다. 인증키를 설정하면 실제 데이터로 대체된다.
 */
public final class DemoFixtures {

    private DemoFixtures() {
    }

    /** 뉴스 게시판 예시 한 건. */
    public record DemoNews(String title, String link, String guid) {
    }

    private static final List<LawApiItem> DEMO_LAWS = List.of(
            law("demo-law-001", "산업안전보건법", "법률", "고용노동부",
                    "제19591호", "20230816", "20240517",
                    "https://www.law.go.kr/법령/산업안전보건법", null),
            law("demo-law-002", "산업안전보건법 시행령", "대통령령", "고용노동부",
                    "제34603호", "20240618", "20240618",
                    "https://www.law.go.kr/법령/산업안전보건법시행령", null),
            law("demo-law-003", "산업안전보건법 시행규칙", "고용노동부령", "고용노동부",
                    "제417호", "20240101", "20240101",
                    "https://www.law.go.kr/법령/산업안전보건법시행규칙", null),
            // 본문(content)이 있는 건은 키워드 검색 데모용이다. 실제 법령
            // 본문 전체가 아니라 해당 키워드가 조문에 들어 있다는 것만
            // 보여주기 위한 발췌문이다.
            law("demo-law-004", "산업안전보건기준에 관한 규칙", "고용노동부령", "고용노동부",
                    "제402호", "20231128", "20231128",
                    "https://www.law.go.kr/법령/산업안전보건기준에관한규칙",
                    "제241조(화재위험 작업 시의 준수사항) 사업주는 통풍이나 환기가 충분하지 않은 장소에서 "
                            + "화재위험작업(용접·용단 등 불꽃이나 화기를 사용하는 작업)을 하는 경우 "
                            + "화재감시자를 배치하여야 한다."),
            law("demo-law-005", "중대재해 처벌 등에 관한 법률", "법률", "법무부",
                    "제18627호", "20220126", "20220127",
                    "https://www.law.go.kr/법령/중대재해처벌등에관한법률", null),

            admrul("demo-admrul-001", "유해위험방지계획서 제출·심사 및 확인에 관한 규칙", "고시", "고용노동부",
                    "고용노동부고시 제2023-31호", "20230630", "20230701",
                    "https://www.law.go.kr/행정규칙/유해위험방지계획서",
                    "제6조(유해위험방지계획서의 내용) 탱크·배관 등의 설비를 해체하거나 정비·보수 작업을 "
                            + "하는 경우로서 화기작업을 수반하는 경우에는 유해위험방지계획서에 "
                            + "화재·폭발 예방대책을 포함하여야 한다."),
            admrul("demo-admrul-002", "관리감독자 안전보건교육 운영지침", "예규", "고용노동부",
                    "고용노동부예규 제220호", "20220310", "20220310",
                    "https://www.law.go.kr/행정규칙/관리감독자안전보건교육운영지침", null),
            admrul("demo-admrul-003", "위험성평가 실시규정", "고시", "고용노동부",
                    "고용노동부고시 제2023-19호", "20230501", "20230501",
                    "https://www.law.go.kr/행정규칙/위험성평가실시규정", null),

            // 아래 두 건은 "신규 제정 고시 자동 탐지" 데모용이다. 등록되지
            // 않은 채로 남겨 두어, 새로고침하면 후보로 잡히는 것을 보여준다.
            admrul("demo-admrul-004", "밀폐공간 작업 유해위험 방지에 관한 고시", "고시", "고용노동부",
                    "고용노동부고시 제2026-4호", "20260115", "20260115",
                    "https://www.law.go.kr/행정규칙/밀폐공간작업유해위험방지에관한고시",
                    "제9조(밀폐공간 화기작업) 밀폐공간에서 화기작업을 실시하는 경우 사전에 가스농도를 "
                            + "측정하고, 화기작업 중에는 지속적으로 환기를 실시하여야 한다."),
            admrul("demo-admrul-005", "중대재해 예방을 위한 안전보건관리체계 구축 지침", "예규", "고용노동부",
                    "고용노동부예규 제245호", "20260302", "20260302",
                    "https://www.law.go.kr/행정규칙/중대재해예방을위한안전보건관리체계구축지침", null),

            // 소관부처가 달라 "고용노동부" 조건에 걸러져야 하는 항목이다.
            // 부처 + 키워드 이중 필터가 부처 조건까지 실제로 보는지 확인용.
            admrul("demo-admrul-006", "화학물질 취급시설 안전관리에 관한 고시", "고시", "환경부",
                    "환경부고시 제2026-11호", "20260210", "20260210",
                    "https://www.law.go.kr/행정규칙/화학물질취급시설안전관리에관한고시", null));

    /**
     * 뉴스 게시판 예시.
     *
     * <p>법령 예시와 달리 인증키 설정과 무관하게 쓰인다. 실제 RSS 를 가져오지
     * 못했을 때(사내망 차단 등) 화면이 비어 보이지 않게 하는 용도다.
     * guid 를 고정해 두어 반복 수집해도 중복 저장되지 않는다.
     */
    private static final Map<String, List<DemoNews>> DEMO_NEWS = Map.of(
            "moel", List.of(
                    new DemoNews("[예시] 고용노동부, 중대재해 예방을 위한 산업안전보건 감독 강화 계획 발표",
                            "https://www.moel.go.kr", "demo-news-moel-1"),
                    new DemoNews("[예시] 고용노동부, 밀폐공간 질식재해 예방 집중 점검 실시",
                            "https://www.moel.go.kr", "demo-news-moel-2"),
                    new DemoNews("[예시] 산업안전보건법 시행규칙 개정안 행정예고",
                            "https://www.moel.go.kr", "demo-news-moel-3")),
            "kosha", List.of(
                    new DemoNews("[예시] 안전보건공단, 여름철 온열질환 예방 안전보건 가이드 배포",
                            "https://www.kosha.or.kr", "demo-news-kosha-1"),
                    new DemoNews("[예시] 안전보건공단, 건설현장 추락재해 예방 특별 캠페인 실시",
                            "https://www.kosha.or.kr", "demo-news-kosha-2"),
                    new DemoNews("[예시] 안전보건공단, 위험성평가 우수사례 공모전 접수",
                            "https://www.kosha.or.kr", "demo-news-kosha-3")),
            "accident", List.of(
                    new DemoNews("[예시] 제조업 사업장 끼임 사고로 중대재해 발생, 관계기관 조사 착수",
                            "https://www.moel.go.kr", "demo-news-accident-1"),
                    new DemoNews("[예시] 건설현장 추락사고 중대재해 판단, 원청 안전보건관리체계 점검",
                            "https://www.moel.go.kr", "demo-news-accident-2"),
                    new DemoNews("[예시] 화학물질 누출사고로 인한 중대산업재해 조사 진행",
                            "https://www.moel.go.kr", "demo-news-accident-3")));

    static List<LawApiItem> search(String sourceType, String query) {
        String q = query == null ? "" : query.trim();
        List<LawApiItem> out = new ArrayList<>();
        for (LawApiItem law : DEMO_LAWS) {
            if (!law.getSourceType().equals(sourceType)) {
                continue;
            }
            if (q.isEmpty() || law.getName().contains(q)) {
                out.add(law);
            }
        }
        return out;
    }

    static LawApiItem getDetail(String sourceType, String externalId) {
        for (LawApiItem law : DEMO_LAWS) {
            if (law.getSourceType().equals(sourceType) && law.getExternalId().equals(externalId)) {
                return law;
            }
        }
        return null;
    }

    public static List<DemoNews> news(String category) {
        return DEMO_NEWS.getOrDefault(category, List.of());
    }

    private static LawApiItem law(String externalId, String name, String category, String department,
                                  String promulgationNo, String promulgationDate,
                                  String enforcementDate, String detailLink, String content) {
        return build(LawApiClient.LAW, externalId, name, category, department,
                promulgationNo, promulgationDate, enforcementDate, detailLink, content);
    }

    private static LawApiItem admrul(String externalId, String name, String category, String department,
                                     String promulgationNo, String promulgationDate,
                                     String enforcementDate, String detailLink, String content) {
        return build(LawApiClient.ADMRUL, externalId, name, category, department,
                promulgationNo, promulgationDate, enforcementDate, detailLink, content);
    }

    private static LawApiItem build(String sourceType, String externalId, String name, String category,
                                    String department, String promulgationNo, String promulgationDate,
                                    String enforcementDate, String detailLink, String content) {
        LawApiItem item = new LawApiItem();
        item.setSourceType(sourceType);
        item.setExternalId(externalId);
        item.setName(name);
        item.setCategory(category);
        item.setDepartment(department);
        item.setPromulgationNo(promulgationNo);
        item.setPromulgationDate(promulgationDate);
        item.setEnforcementDate(enforcementDate);
        item.setDetailLink(detailLink);
        item.setContent(content);
        item.setArticleContentLen(content == null ? 0 : content.length());
        return item;
    }
}
