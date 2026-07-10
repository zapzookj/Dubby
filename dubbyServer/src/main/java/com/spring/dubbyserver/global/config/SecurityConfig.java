package com.spring.dubbyserver.global.config;

import com.spring.dubbyserver.global.error.ErrorCode;
import com.spring.dubbyserver.global.error.ErrorResponse;
import com.spring.dubbyserver.global.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/webhooks/revenuecat",
                                "/api/v1/health",
                                "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(jsonAuthEntryPoint()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 인증 실패도 공통 에러 포맷 { code, message, derbyMessage } 유지 */
    private AuthenticationEntryPoint jsonAuthEntryPoint() {
        return (request, response, authException) -> {
            ErrorCode code = JwtAuthenticationFilter.resolveAuthError(request);
            ErrorResponse body = ErrorResponse.of(code);
            response.setStatus(code.getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                    {"code":"%s","message":"%s","derbyMessage":"%s"}"""
                    .formatted(body.code(), escape(body.message()), escape(body.derbyMessage())));
        };
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
