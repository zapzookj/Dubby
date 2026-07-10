package com.spring.dubbyserver.global.config;

import com.spring.dubbyserver.domain.metrics.UserActivityRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final UserActivityRecorder userActivityRecorder;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userActivityRecorder).addPathPatterns("/api/v1/**");
    }
}
