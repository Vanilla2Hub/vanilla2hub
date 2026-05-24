package com.vanilla2hub.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
public class JwtCookieFilter extends OncePerRequestFilter {

    private static final String COOKIE_NAME = "access_token";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Authorization 헤더가 이미 있으면 통과 (Postman 등 직접 Bearer 사용)
        if (StringUtils.hasText(request.getHeader(HttpHeaders.AUTHORIZATION))) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractTokenFromCookie(request);
        if (StringUtils.hasText(token)) {
            filterChain.doFilter(new BearerTokenRequestWrapper(request, token), response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private static class BearerTokenRequestWrapper extends HttpServletRequestWrapper {

        private final String token;

        BearerTokenRequestWrapper(HttpServletRequest request, String token) {
            super(request);
            this.token = token;
        }

        @Override
        public String getHeader(String name) {
            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                return BEARER_PREFIX + token;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(BEARER_PREFIX + token));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            if (!names.contains(HttpHeaders.AUTHORIZATION)) {
                names.add(HttpHeaders.AUTHORIZATION);
            }
            return Collections.enumeration(names);
        }
    }
}
