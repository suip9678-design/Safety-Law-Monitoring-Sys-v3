package com.safetylaw.monitor.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 외부 조회용 HTTP 클라이언트.
 *
 * <p>국가법령정보 공동활용 API 와 뉴스 RSS 를 읽는 데 쓴다. 두 곳 모두
 * 응답이 느릴 수 있어 타임아웃을 넉넉히 두되, 응답이 없을 때 스케줄 작업이
 * 무한정 붙잡히지 않도록 상한을 반드시 건다.
 *
 * <p>사내망에서 TLS 를 중간에서 검사하는 장비를 쓰는 경우 인증서 오류가
 * 날 수 있다. 그때는 해당 사내 인증서를 JDK 신뢰 저장소(cacerts)에
 * 등록하거나 실행 시 -Djavax.net.ssl.trustStore 로 지정해야 한다.
 * (기존 Python 버전이 truststore 패키지로 처리하던 부분에 해당한다.)
 */
@Configuration
public class HttpClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    @Bean
    RestClient lawApiRestClient(AppProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.lawApiBaseUrl())
                .requestFactory(requestFactory())
                .build();
    }

    /** 뉴스 RSS 는 외부 주소가 매번 달라 baseUrl 없이 쓴다. */
    @Bean
    RestClient plainRestClient() {
        return RestClient.builder()
                .requestFactory(requestFactory())
                .build();
    }

    private ClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }
}
