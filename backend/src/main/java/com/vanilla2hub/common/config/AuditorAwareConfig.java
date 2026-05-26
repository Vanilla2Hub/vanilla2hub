package com.vanilla2hub.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

@Configuration
public class AuditorAwareConfig {

    @Bean
    @SuppressWarnings("null")
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return Optional.of("system");
            }
            if (auth.getPrincipal() instanceof Jwt jwt) {
                String sub = jwt.getSubject();
                return Optional.ofNullable(sub).or(() -> Optional.of("system"));
            }
            return Optional.of(auth.getName());
        };
    }
}
