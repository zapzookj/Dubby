package com.spring.dubbyserver.domain.push;

import com.spring.dubbyserver.global.config.DubbyProperties;
import com.spring.dubbyserver.global.error.DubbyException;
import com.spring.dubbyserver.global.error.ErrorCode;
import com.spring.dubbyserver.global.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
public class PushController {

    private final JdbcClient jdbc;
    private final DubbyProperties properties;

    public record TokenRequest(@NotBlank String expoPushToken, String deviceId, String platform) {}

    /** 멱등 upsert — 앱 시작 시마다 호출 (토큰 로테이션 대응) */
    @PostMapping("/tokens")
    public Map<String, Object> register(@AuthenticationPrincipal AuthUser authUser,
                                        @Valid @RequestBody TokenRequest request) {
        if (!request.expoPushToken().startsWith("ExponentPushToken[")) {
            throw new DubbyException(ErrorCode.PUSH_INVALID_TOKEN);
        }
        String platform = "IOS".equalsIgnoreCase(request.platform()) ? "IOS" : "ANDROID";
        jdbc.sql("""
                INSERT INTO push_tokens (user_id, expo_push_token, device_id, platform, is_active, last_used_at)
                VALUES (:userId, :token, :deviceId, :platform, TRUE, now())
                ON CONFLICT (expo_push_token) DO UPDATE SET
                    user_id = :userId, device_id = :deviceId, is_active = TRUE, last_used_at = now()
                """)
                .param("userId", authUser.id())
                .param("token", request.expoPushToken())
                .param("deviceId", request.deviceId())
                .param("platform", platform)
                .update();
        // 설정 행 보장 (기본값 yml)
        jdbc.sql("""
                INSERT INTO push_settings (user_id, enabled, max_daily_count)
                VALUES (:userId, TRUE, :defaultCount) ON CONFLICT (user_id) DO NOTHING
                """)
                .param("userId", authUser.id())
                .param("defaultCount", properties.push().defaultDailyCount())
                .update();
        return Map.of("registered", true);
    }

    /** DELETE body 회피 관행 — POST /tokens/delete */
    @PostMapping("/tokens/delete")
    public Map<String, Object> delete(@AuthenticationPrincipal AuthUser authUser,
                                      @Valid @RequestBody TokenRequest request) {
        jdbc.sql("DELETE FROM push_tokens WHERE user_id = :userId AND expo_push_token = :token")
                .param("userId", authUser.id())
                .param("token", request.expoPushToken())
                .update();
        return Map.of("deleted", true);
    }

    @GetMapping("/settings")
    public Map<String, Object> getSettings(@AuthenticationPrincipal AuthUser authUser) {
        return jdbc.sql("SELECT enabled, max_daily_count FROM push_settings WHERE user_id = :userId")
                .param("userId", authUser.id())
                .query((rs, i) -> Map.<String, Object>of(
                        "enabled", rs.getBoolean("enabled"),
                        "maxDailyCount", rs.getInt("max_daily_count")))
                .optional()
                .orElse(Map.of("enabled", true, "maxDailyCount", properties.push().defaultDailyCount()));
    }

    public record SettingsRequest(Boolean enabled, Integer maxDailyCount) {}

    /** enabled=false면 발송 배치에서 완전 제외 — 장난은 해도 OFF는 존중 */
    @PutMapping("/settings")
    public Map<String, Object> putSettings(@AuthenticationPrincipal AuthUser authUser,
                                           @RequestBody SettingsRequest request) {
        int count = request.maxDailyCount() != null ? request.maxDailyCount()
                : properties.push().defaultDailyCount();
        if (count < 1 || count > properties.push().maxDailyCount()) {
            throw new DubbyException(ErrorCode.PUSH_INVALID_DAILY_COUNT);
        }
        boolean enabled = request.enabled() == null || request.enabled();
        jdbc.sql("""
                INSERT INTO push_settings (user_id, enabled, max_daily_count, updated_at)
                VALUES (:userId, :enabled, :count, now())
                ON CONFLICT (user_id) DO UPDATE SET
                    enabled = :enabled, max_daily_count = :count, updated_at = now()
                """)
                .param("userId", authUser.id())
                .param("enabled", enabled)
                .param("count", count)
                .update();
        return Map.of("enabled", enabled, "maxDailyCount", count);
    }

    /** 푸시 탭 오픈 추적 (클릭률 지표) */
    @PostMapping("/logs/{pushLogId}/open")
    public Map<String, Object> open(@AuthenticationPrincipal AuthUser authUser,
                                    @PathVariable Long pushLogId) {
        int updated = jdbc.sql("""
                UPDATE push_send_logs SET opened_at = COALESCE(opened_at, now())
                WHERE id = :id AND user_id = :userId
                """)
                .param("id", pushLogId)
                .param("userId", authUser.id())
                .update();
        if (updated == 0) {
            throw new DubbyException(ErrorCode.PUSH_LOG_NOT_FOUND);
        }
        return Map.of("opened", true);
    }
}
