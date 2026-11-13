package it.plannershop.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import it.plannershop.auth.AuthDtos.ChangePasswordRequest;
import it.plannershop.auth.AuthDtos.CurrentUserDto;
import it.plannershop.auth.AuthDtos.LoginRequest;
import it.plannershop.auth.AuthDtos.LoginResponse;
import it.plannershop.auth.AuthDtos.Session;
import it.plannershop.auth.AuthService.ChoiceRequired;
import it.plannershop.auth.AuthService.SignedIn;
import it.plannershop.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Authentication APIs.
 *
 * <p>The access token goes in the response body: the client keeps it in memory
 * and sends it in the Authorization header. The refresh token goes in an
 * {@code HttpOnly} cookie (JavaScript cannot read it) valid only for {@code /api/auth}.
 */
@RestController
public class AuthController {

    static final String REFRESH_COOKIE = "ps_refresh";
    private static final String COOKIE_PATH = "/api/auth";

    private final AuthService auth;

    AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/api/auth/login")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return switch (auth.login(body.email(), body.password(), body.tenantId(), request.getRemoteAddr(), userAgent)) {
            case SignedIn(Session s) -> withCookie(s, request).body(LoginResponse.of(s));
            case ChoiceRequired(var options) -> ResponseEntity.ok(LoginResponse.choice(options));
        };
    }

    @PostMapping("/api/auth/refresh")
    ResponseEntity<LoginResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        Session s = auth.refresh(refreshToken, userAgent);
        return withCookie(s, request).body(LoginResponse.of(s));
    }

    @PostMapping("/api/auth/logout")
    ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletRequest request) {
        auth.logout(refreshToken);
        ResponseCookie empty = cookie("", 0, request);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, empty.toString()).build();
    }

    @GetMapping("/api/me")
    CurrentUserDto me() {
        return auth.currentUser(CurrentUser.id(), CurrentUser.tenantId().orElse(null));
    }

    @PutMapping("/api/auth/password")
    ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest body) {
        auth.changePassword(CurrentUser.id(), body.currentPassword(), body.newPassword());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity.BodyBuilder withCookie(Session s, HttpServletRequest request) {
        ResponseCookie c = cookie(s.refreshToken(), auth.refreshTtl().toSeconds(), request);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, c.toString());
    }

    /**
     * Secure only if the request arrives over HTTPS (also through a proxy, header
     * X-Forwarded-Proto): online the cookie travels only encrypted, while locally
     * on http://localhost it still works.
     */
    private static ResponseCookie cookie(String value, long maxAgeSeconds, HttpServletRequest request) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .build();
    }
}
