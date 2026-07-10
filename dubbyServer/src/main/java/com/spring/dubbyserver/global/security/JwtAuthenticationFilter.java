package com.spring.dubbyserver.global.security;

import com.spring.dubbyserver.global.error.DubbyException;
import com.spring.dubbyserver.global.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Authorization: Bearer {jwt} 검증 필터.
 * 토큰 문제는 request attribute에 ErrorCode를 실어 EntryPoint에서 공통 에러 포맷으로 응답한다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTR = "dubby.authErrorCode";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                UUID userId = jwtProvider.parseUserId(token);
                var authentication = new UsernamePasswordAuthenticationToken(
                        new AuthUser(userId), null, AuthorityUtils.createAuthorityList("ROLE_USER"));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (DubbyException e) {
                request.setAttribute(AUTH_ERROR_ATTR, e.getErrorCode());
            }
        }
        filterChain.doFilter(request, response);
    }

    /** EntryPoint에서 사용: 필터가 기록한 에러 코드 or 기본값 */
    public static ErrorCode resolveAuthError(HttpServletRequest request) {
        Object attr = request.getAttribute(AUTH_ERROR_ATTR);
        return attr instanceof ErrorCode code ? code : ErrorCode.AUTH_TOKEN_INVALID;
    }
}
