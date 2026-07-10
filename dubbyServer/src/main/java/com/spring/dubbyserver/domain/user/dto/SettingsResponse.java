package com.spring.dubbyserver.domain.user.dto;

import com.spring.dubbyserver.domain.user.User;

import java.util.Map;

public record SettingsResponse(
        String nickname,
        String timezone,
        Map<String, Object> prefs
) {
    public static SettingsResponse from(User user) {
        return new SettingsResponse(user.getNickname(), user.getTimezone(), user.getPrefs());
    }
}
