package com.safetylaw.monitor.lawapi;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 현재 설정된 API 인증키에 맞는 클라이언트를 만든다.
 *
 * <p>인증키는 설정 화면에서 실행 중에도 바꿀 수 있으므로, 클라이언트를
 * 하나 만들어 재사용하지 않고 호출 시점의 인증키로 매번 고른다.
 * 실제 통신을 담당하는 {@link RestClient} 는 재사용되므로 연결은 유지된다.
 */
@Component
public class LawApiClientFactory {

    private final RestClient lawApiRestClient;

    public LawApiClientFactory(@Qualifier("lawApiRestClient") RestClient lawApiRestClient) {
        this.lawApiRestClient = lawApiRestClient;
    }

    /** 인증키가 비어 있으면 예시 데이터로 답하는 클라이언트를 돌려준다. */
    public LawApiClient create(String oc) {
        if (oc == null || oc.isBlank()) {
            return new DemoLawApiClient();
        }
        return new RealLawApiClient(lawApiRestClient, oc);
    }
}
