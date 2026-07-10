package com.spring.dubbyserver.domain.billing;

import com.spring.dubbyserver.global.config.DubbyProperties;
import com.spring.dubbyserver.global.error.DubbyException;
import com.spring.dubbyserver.global.error.ErrorCode;
import com.spring.dubbyserver.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final DubbyProperties properties;

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal AuthUser authUser) {
        BillingService.BillingStatus status = billingService.status(authUser.id());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tier", status.tier().name());
        body.put("chatDailyLimit", billingService.chatDailyLimit(status.tier()));
        body.put("expiresAt", status.expiresAt() != null ? status.expiresAt().toString() : null);
        body.put("willRenew", status.willRenew());
        body.put("coffeeActiveUntil",
                status.coffeeActiveUntil() != null ? status.coffeeActiveUntil().toString() : null);
        return body;
    }

    /**
     * 구매 직후 클라 트리거 동기화 (웹훅 지연 대비 벨트+멜빵).
     * RC REST API 키(RC_API_KEY env) 미설정 시 현재 상태만 반환 — 웹훅이 유일 경로가 된다.
     */
    @PostMapping("/sync")
    public Map<String, Object> sync(@AuthenticationPrincipal AuthUser authUser) {
        String apiKey = System.getenv("RC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.info("billing/sync: RC_API_KEY 미설정 — 현재 상태 반환");
            return me(authUser);
        }
        try {
            // RC 구독자 조회 → 웹훅과 동일 반영은 웹훅에 맡기고, 여기서는 조회 성공 여부만 확인
            RestClient.create("https://api.revenuecat.com/v1")
                    .get().uri("/subscribers/{id}", authUser.id().toString())
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve().body(Map.class);
            // TODO(운영): 응답의 entitlements를 즉시 반영하는 경로 추가 (웹훅 지연이 실측되면)
            return me(authUser);
        } catch (Exception e) {
            log.warn("billing/sync 실패: {}", e.getMessage());
            throw new DubbyException(ErrorCode.BILLING_SYNC_FAILED);
        }
    }
}
