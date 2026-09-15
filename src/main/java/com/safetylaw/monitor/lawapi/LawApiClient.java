package com.safetylaw.monitor.lawapi;

import java.util.List;

/**
 * 국가법령정보센터(law.go.kr) 공동활용 API 클라이언트.
 *
 * <p>대상은 두 가지다.
 * <ul>
 *   <li>{@code law}    - 법률/시행령/시행규칙 등 "법령"</li>
 *   <li>{@code admrul} - 고시/예규/훈령 등 "행정규칙"</li>
 * </ul>
 *
 * <p>API 인증키(OC)가 없으면 예시 데이터로 답하는 구현이 대신 쓰인다.
 */
public interface LawApiClient {

    String LAW = "law";
    String ADMRUL = "admrul";

    List<LawApiItem> search(String sourceType, String query, int display, int page);

    default List<LawApiItem> search(String sourceType, String query) {
        return search(sourceType, query, 20, 1);
    }

    /**
     * 목록 조회의 전체 건수. 전체 법령 캐시의 진행률 표시에 쓴다.
     *
     * @return 건수를 확인할 수 없으면 null. 이 경우 호출 측은 총 건수 없이
     *         진행 건수만 표시하고 기능 자체는 계속 동작한다.
     */
    Integer countTotal(String sourceType, String query);

    /**
     * 상세 조회. 본문(조문)까지 포함해 가져온다.
     *
     * @param efYd 시행일자 스냅샷 지정(법령에만 해당). 없으면 null
     * @return 이름조차 못 읽은 경우 null
     */
    LawApiItem getDetail(String sourceType, String externalId, String efYd);
}
