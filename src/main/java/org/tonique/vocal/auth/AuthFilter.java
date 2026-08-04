package org.tonique.vocal.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthFilter extends OncePerRequestFilter {

    static final String COOKIE_NAME = "session";
    private static final String LOGIN_PATH = "/api/auth/login";

    private final Set<String> validTokens = ConcurrentHashMap.newKeySet();

    String issueToken() {
        String token = UUID.randomUUID().toString();
        validTokens.add(token);
        return token;
    }

    void revokeToken(String token) {
        validTokens.remove(token);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!LOGIN_PATH.equals(request.getRequestURI()) && !isAuthenticated(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Необхідна автентифікація\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isAuthenticated(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName()) && validTokens.contains(cookie.getValue())) {
                return true;
            }
        }
        return false;
    }
}
