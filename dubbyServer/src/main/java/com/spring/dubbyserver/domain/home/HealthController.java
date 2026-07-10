package com.spring.dubbyserver.domain.home;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "derbyMessage", "더비가 살아있는 척에 성공했습니다.");
    }
}
