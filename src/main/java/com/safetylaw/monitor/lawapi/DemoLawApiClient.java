package com.safetylaw.monitor.lawapi;

import java.util.List;

/** API 인증키가 없을 때 예시 데이터로 답하는 구현. */
class DemoLawApiClient implements LawApiClient {

    @Override
    public List<LawApiItem> search(String sourceType, String query, int display, int page) {
        if (page > 1) {
            // 예시 데이터는 몇 건뿐이라 첫 페이지에서 끝난다. 페이지를 계속
            // 넘기며 훑는 기능(전체 법령 캐시)을 예시 모드에서 돌려도
            // 끝없이 반복되지 않도록 두 번째 페이지부터는 빈 목록을 준다.
            return List.of();
        }
        List<LawApiItem> results = DemoFixtures.search(sourceType, query);
        return results.size() > display ? results.subList(0, display) : results;
    }

    @Override
    public Integer countTotal(String sourceType, String query) {
        return DemoFixtures.search(sourceType, query).size();
    }

    @Override
    public LawApiItem getDetail(String sourceType, String externalId, String efYd) {
        return DemoFixtures.getDetail(sourceType, externalId);
    }
}
