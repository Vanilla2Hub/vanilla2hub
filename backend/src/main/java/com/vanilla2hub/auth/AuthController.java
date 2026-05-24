package com.vanilla2hub.auth;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String COOKIE_NAME = "access_token";

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    // 클라이언트가 Authorization: Bearer <keycloak_token> 으로 호출
    // Spring Security가 JWT 검증 후 이 메서드에 도달
    @PostMapping("/session")
    public ResponseEntity<Void> createSession(@AuthenticationPrincipal Jwt jwt,
                                              HttpServletResponse response) {
        long maxAge = jwt.getExpiresAt() != null
                ? jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond()
                : 3600L;

        ResponseCookie cookie = buildCookie(jwt.getTokenValue(), maxAge);
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/session")
    public ResponseEntity<Void> deleteSession(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildCookie("", 0).toString());
        return ResponseEntity.noContent().build();
    }

    private ResponseCookie buildCookie(String value, long maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
    }
}
