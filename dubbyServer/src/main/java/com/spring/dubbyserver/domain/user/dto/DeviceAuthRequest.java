package com.spring.dubbyserver.domain.user.dto;

import com.spring.dubbyserver.domain.user.User.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeviceAuthRequest(
        @NotBlank @Size(max = 128) String deviceId,
        @NotNull Platform platform,
        @NotBlank @Size(max = 50) String timezone,
        @Size(max = 10) String locale,
        @Size(max = 20) String appVersion
) {}
