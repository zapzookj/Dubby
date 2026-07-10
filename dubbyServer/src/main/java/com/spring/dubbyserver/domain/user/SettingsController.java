package com.spring.dubbyserver.domain.user;

import com.spring.dubbyserver.domain.user.dto.SettingsPatchRequest;
import com.spring.dubbyserver.domain.user.dto.SettingsResponse;
import com.spring.dubbyserver.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SettingsController {

    private final UserService userService;

    @GetMapping("/settings")
    public SettingsResponse getSettings(@AuthenticationPrincipal AuthUser authUser) {
        return userService.getSettings(authUser.id());
    }

    @PatchMapping("/settings")
    public SettingsResponse patchSettings(@AuthenticationPrincipal AuthUser authUser,
                                          @RequestBody SettingsPatchRequest request) {
        return userService.patchSettings(authUser.id(), request);
    }

    /** 계정 + 전체 데이터 삭제 — 장난 금지 구역: 명확한 안내만 */
    @DeleteMapping("/users/me")
    public Map<String, Object> deleteAccount(@AuthenticationPrincipal AuthUser authUser) {
        userService.deleteAccount(authUser.id());
        return Map.of(
                "deleted", true,
                "message", "계정과 모든 데이터가 삭제되었습니다.");
    }
}
