package com.spring.dubbyserver.domain.user.dto;

import java.util.Map;

/** 부분 갱신 — null 필드는 변경하지 않음 */
public record SettingsPatchRequest(
        String nickname,
        String timezone,
        Map<String, Object> prefs
) {}
