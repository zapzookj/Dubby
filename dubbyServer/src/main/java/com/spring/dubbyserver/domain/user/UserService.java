package com.spring.dubbyserver.domain.user;

import com.spring.dubbyserver.domain.user.dto.SettingsPatchRequest;
import com.spring.dubbyserver.domain.user.dto.SettingsResponse;
import com.spring.dubbyserver.global.config.DubbyProperties;
import com.spring.dubbyserver.global.error.DubbyException;
import com.spring.dubbyserver.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DubbyProperties properties;

    @Transactional(readOnly = true)
    public User getActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DubbyException(ErrorCode.AUTH_TOKEN_INVALID, "User not found"));
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new DubbyException(ErrorCode.AUTH_TOKEN_INVALID, "User is being deleted");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public SettingsResponse getSettings(UUID userId) {
        return SettingsResponse.from(getActiveUser(userId));
    }

    /** 부분 갱신 — null 필드는 변경하지 않음. derby_system_spec_v1.md §5.7 */
    @Transactional
    public SettingsResponse patchSettings(UUID userId, SettingsPatchRequest request) {
        User user = getActiveUser(userId);

        if (request.nickname() != null) {
            if (request.nickname().length() > properties.user().nicknameMaxLength()) {
                throw new DubbyException(ErrorCode.SETTINGS_NICKNAME_TOO_LONG);
            }
            user.changeNickname(request.nickname().isBlank() ? null : request.nickname().trim());
        }

        if (request.timezone() != null && !request.timezone().equals(user.getTimezone())) {
            try {
                ZoneId.of(request.timezone());
            } catch (DateTimeException e) {
                throw new DubbyException(ErrorCode.SETTINGS_INVALID_TIMEZONE);
            }
            // 타임존 변경 빈도 가드 — 로컬 자정 리셋(채팅 쿼터) 악용 방지 (스펙 §5.7)
            Instant changedAt = user.getTimezoneChangedAt();
            if (changedAt != null
                    && changedAt.plus(properties.user().timezoneChangeMinInterval()).isAfter(Instant.now())) {
                throw new DubbyException(ErrorCode.SETTINGS_TIMEZONE_CHANGE_TOO_OFTEN);
            }
            user.changeTimezone(request.timezone());
        }

        if (request.prefs() != null) {
            user.mergePrefs(request.prefs());
        }

        return SettingsResponse.from(user);
    }

    /**
     * 계정 + 전체 데이터 즉시 삭제 (iOS 심사 필수 — 스펙 §5.9).
     * DELETING 마킹 후 users 행 삭제 → 자식 테이블은 DB ON DELETE CASCADE.
     * revenuecat_events는 user FK가 없어 감사 기록으로 보존된다.
     */
    @Transactional
    public void deleteAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DubbyException(ErrorCode.AUTH_TOKEN_INVALID, "User not found"));
        user.markDeleting();
        userRepository.flush();
        userRepository.delete(user);
    }
}
