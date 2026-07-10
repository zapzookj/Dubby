package com.spring.dubbyserver.domain.push;

import com.spring.dubbyserver.global.security.AuthUser;
import com.spring.dubbyserver.infra.expo.ExpoPushClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** local 전용 강제 발송 — 창/쿼터/쿨다운 무시, 수신·딥링크 즉시 검증용 (SPIKE-B) */
@Profile("local")
@RestController
@RequestMapping("/api/v1/admin/push")
@RequiredArgsConstructor
public class PushTestController {

    private final JdbcClient jdbc;
    private final ExpoPushClient expo;

    @PostMapping("/test")
    public Map<String, Object> test(@AuthenticationPrincipal AuthUser authUser) {
        var token = jdbc.sql("""
                SELECT expo_push_token FROM push_tokens
                WHERE user_id = :userId AND is_active ORDER BY last_used_at DESC LIMIT 1
                """)
                .param("userId", authUser.id())
                .query(String.class).optional();
        if (token.isEmpty()) {
            return Map.of("sent", false, "reason", "활성 푸시 토큰 없음");
        }
        List<ExpoPushClient.Ticket> tickets = expo.send(List.of(new ExpoPushClient.PushMessage(
                token.get(), "알림입니다", "내용은 없습니다. (테스트 발송)",
                Map.of("deeplink", "dubby://tasks"))));
        return Map.of("sent", true, "ticket", tickets.get(0));
    }
}
