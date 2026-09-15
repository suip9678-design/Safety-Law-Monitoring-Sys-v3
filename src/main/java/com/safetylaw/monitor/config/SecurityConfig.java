package com.safetylaw.monitor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 대시보드 접근 보호.
 *
 * <p>이 시스템은 사내 안전보건 문서 목록과, 메일 발송·추적 법령 변경 같은
 * 설정을 담고 있어 최소한의 로그인이 필요하다. 다만 개인 PC에서 혼자 쓰는
 * 경우까지 매번 로그인을 요구하면 불편하므로, 기존 Python 버전과 동일하게
 * <b>아이디와 비밀번호가 모두 설정된 경우에만</b> 로그인을 요구한다.
 *
 * <p>상태 점검용 {@code /api/health} 는 로그인 없이 열어 둔다.
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private static final String REALM = "Safety Law Tracker";

    private final AppProperties properties;

    public SecurityConfig(AppProperties properties) {
        this.properties = properties;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean loginRequired = properties.dashboard().loginRequired();

        // 브라우저의 JS 가 호출하는 JSON API 이고 세션을 쓰지 않으므로 CSRF 토큰은 사용하지 않는다.
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!loginRequired) {
            log.info("대시보드 로그인 보호가 꺼져 있습니다. "
                    + "(app.dashboard.username 과 app.dashboard.password 를 모두 설정하면 켜집니다)");
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        log.info("대시보드 로그인 보호가 켜져 있습니다. 사용자: {}", properties.dashboard().username());
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll()
                        .anyRequest().authenticated())
            .httpBasic(basic -> basic.realmName(REALM));

        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService() {
        AppProperties.Dashboard dashboard = properties.dashboard();
        if (!dashboard.loginRequired()) {
            return new InMemoryUserDetailsManager();
        }
        // 비밀번호는 설정 파일/환경변수에서 평문으로 받는다(기존 버전과 동일).
        // {noop} 은 "별도 해시를 쓰지 않는다"는 Spring Security 표기법이다.
        return new InMemoryUserDetailsManager(
                User.withUsername(dashboard.username())
                        .password("{noop}" + dashboard.password())
                        .roles("USER")
                        .build());
    }
}
