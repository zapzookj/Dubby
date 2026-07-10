package com.spring.dubbyserver.global.security;

import java.util.UUID;

/** SecurityContext principal — 인증된 사용자 ID 래핑 */
public record AuthUser(UUID id) {}
