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

import it.plannershop.auth.AuthDtos.CambioPasswordRequest;
import it.plannershop.auth.AuthDtos.LoginRequest;
import it.plannershop.auth.AuthDtos.LoginResponse;
import it.plannershop.auth.AuthDtos.Sessione;
import it.plannershop.auth.AuthDtos.UtenteCorrente;
import it.plannershop.auth.AuthService.Accesso;
import it.plannershop.auth.AuthService.SceltaRichiesta;
import it.plannershop.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * API di autenticazione.
 *
 * <p>L'access token va nel body della risposta: il client lo tiene in memoria e
 * lo manda nell'header Authorization. Il refresh token va in un cookie
 * {@code HttpOnly} (JavaScript non può leggerlo) valido solo per {@code /api/auth}.
 */
@RestController
public class AuthController {

    static final String COOKIE_REFRESH = "ps_refresh";
    private static final String COOKIE_PATH = "/api/auth";

    private final AuthService auth;

    AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/api/auth/login")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return switch (auth.login(body.email(), body.password(), body.tenantId(), request.getRemoteAddr(), userAgent)) {
            case Accesso(Sessione s) -> conCookie(s, request).body(LoginResponse.di(s));
            case SceltaRichiesta(var opzioni) -> ResponseEntity.ok(LoginResponse.scelta(opzioni));
        };
    }

    @PostMapping("/api/auth/refresh")
    ResponseEntity<LoginResponse> refresh(@CookieValue(name = COOKIE_REFRESH, required = false) String refreshToken,
            HttpServletRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        Sessione s = auth.refresh(refreshToken, userAgent);
        return conCookie(s, request).body(LoginResponse.di(s));
    }

    @PostMapping("/api/auth/logout")
    ResponseEntity<Void> logout(@CookieValue(name = COOKIE_REFRESH, required = false) String refreshToken,
            HttpServletRequest request) {
        auth.logout(refreshToken);
        ResponseCookie vuoto = cookie("", 0, request);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, vuoto.toString()).build();
    }

    @GetMapping("/api/me")
    UtenteCorrente me() {
        return auth.utenteCorrente(CurrentUser.id(), CurrentUser.tenantId().orElse(null));
    }

    @PutMapping("/api/auth/password")
    ResponseEntity<Void> cambiaPassword(@Valid @RequestBody CambioPasswordRequest body) {
        auth.cambiaPassword(CurrentUser.id(), body.passwordAttuale(), body.nuovaPassword());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity.BodyBuilder conCookie(Sessione s, HttpServletRequest request) {
        ResponseCookie c = cookie(s.refreshToken(), auth.durataRefresh().toSeconds(), request);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, c.toString());
    }

    /**
     * Secure solo se la richiesta arriva in HTTPS (anche tramite proxy, header
     * X-Forwarded-Proto): online il cookie viaggia solo cifrato, in locale su
     * http://localhost funziona comunque.
     */
    private static ResponseCookie cookie(String valore, long maxAgeSecondi, HttpServletRequest request) {
        return ResponseCookie.from(COOKIE_REFRESH, valore)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAgeSecondi)
                .build();
    }
}
