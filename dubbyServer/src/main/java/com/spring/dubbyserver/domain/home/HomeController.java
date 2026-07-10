package com.spring.dubbyserver.domain.home;

import com.spring.dubbyserver.domain.home.dto.HomeResponse;
import com.spring.dubbyserver.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/home")
    public HomeResponse home(@AuthenticationPrincipal AuthUser authUser) {
        return homeService.getHome(authUser.id());
    }
}
