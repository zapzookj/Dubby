package com.spring.dubbyserver.domain.user.dto;

import java.util.UUID;

public record DeviceAuthResponse(
        UUID userId,
        boolean isNewUser,
        String accessToken,
        long expiresIn
) {}
