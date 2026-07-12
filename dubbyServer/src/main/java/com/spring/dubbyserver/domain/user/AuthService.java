package com.spring.dubbyserver.domain.user;

import com.spring.dubbyserver.domain.user.dto.DeviceAuthRequest;
import com.spring.dubbyserver.domain.user.dto.DeviceAuthResponse;
import com.spring.dubbyserver.global.error.DubbyException;
import com.spring.dubbyserver.global.error.ErrorCode;
import com.spring.dubbyserver.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    /** 디바이스 게스트 등록/재로그인 (멱등) — derby_system_spec_v1.md §4 */
    @Transactional
    public DeviceAuthResponse registerOrLogin(DeviceAuthRequest request) {
        validateDeviceId(request.deviceId());
        validateTimezone(request.timezone());

        boolean[] created = {false};
        User user = userRepository.findByDeviceId(request.deviceId())
                .orElseGet(() -> {
                    created[0] = true;
                    try {
                        return userRepository.saveAndFlush(User.builder()
                                .deviceId(request.deviceId())
                                .locale(normalizeLocale(request.locale()))
                                .timezone(request.timezone())
                                .platform(request.platform())
                                .appVersion(request.appVersion())
                                .build());
                    } catch (DataIntegrityViolationException e) {
                        // 동시 등록 경쟁: 먼저 들어간 계정으로 로그인
                        created[0] = false;
                        return userRepository.findByDeviceId(request.deviceId()).orElseThrow();
                    }
                });

        user.touch(request.appVersion());

        String token = jwtProvider.issue(user.getId(), user.getDeviceId());
        return new DeviceAuthResponse(user.getId(), created[0], token, jwtProvider.ttlSeconds());
    }

    /**
     * BCP-47 태그('ko-KR', 'en_US')를 언어 코드('ko')로 정규화.
     * 템플릿 locale('ko')과의 매칭 기준 — 미정규화 시 템플릿 조회가 전부 빈 결과가 된다.
     */
    static String normalizeLocale(String raw) {
        if (raw == null || raw.isBlank()) return "ko";
        String language = raw.split("[-_]")[0].toLowerCase();
        return language.isBlank() ? "ko" : language;
    }

    private void validateDeviceId(String deviceId) {
        try {
            UUID.fromString(deviceId);
        } catch (IllegalArgumentException e) {
            throw new DubbyException(ErrorCode.AUTH_INVALID_DEVICE_ID);
        }
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new DubbyException(ErrorCode.AUTH_INVALID_TIMEZONE);
        }
    }
}
