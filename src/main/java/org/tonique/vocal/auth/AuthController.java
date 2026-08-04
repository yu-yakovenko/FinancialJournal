package org.tonique.vocal.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthFilter authFilter;
    private final String configuredPassword;

    public AuthController(AuthFilter authFilter, @Value("${app.auth.password}") String configuredPassword) {
        this.authFilter = authFilter;
        this.configuredPassword = configuredPassword;
    }

    public record LoginRequest(String password) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        if (request.password() == null || !MessageDigest.isEqual(
                request.password().getBytes(StandardCharsets.UTF_8),
                configuredPassword.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(401).body(Map.of("error", "Невірний пароль"));
        }

        String token = authFilter.issueToken();
        ResponseCookie cookie = ResponseCookie.from(AuthFilter.COOKIE_NAME, token)
                .httpOnly(true)
                .secure(servletRequest.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(30))
                .build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = AuthFilter.COOKIE_NAME, required = false) String token) {
        if (token != null) {
            authFilter.revokeToken(token);
        }
        ResponseCookie cleared = ResponseCookie.from(AuthFilter.COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cleared.toString()).build();
    }
}
