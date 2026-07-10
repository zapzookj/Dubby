package com.spring.dubbyserver.domain.user;

import com.spring.dubbyserver.domain.user.dto.DeviceAuthRequest;
import com.spring.dubbyserver.domain.user.dto.DeviceAuthResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/device")
    public DeviceAuthResponse device(@Valid @RequestBody DeviceAuthRequest request) {
        return authService.registerOrLogin(request);
    }
}
