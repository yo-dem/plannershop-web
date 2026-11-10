package it.plannershop.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Regole di accesso alle API.
 *
 * <ul>
 * <li>Ogni richiesta (tranne login/refresh/logout, health e documentazione)
 * deve avere l'header {@code Authorization: Bearer <access token>}.</li>
 * <li>Nessuna sessione lato server (stateless): tutto è nel token.</li>
 * <li>CSRF disattivato: le API non usano cookie per autenticarsi; l'unico cookie,
 * il refresh token, è {@code SameSite=Strict} e limitato a {@code /api/auth}.</li>
 * <li>I permessi per ruolo si dichiarano sui metodi con {@code @PreAuthorize}.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers("/actuator/health/**", "/error").permitAll()
                        .requestMatchers("/api/v3/api-docs/**", "/api/swagger-ui/**", "/api/swagger-ui.html").permitAll()
                        .requestMatchers("/api/platform/**").hasRole("SUPERADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));
        return http.build();
    }

    /** Claim "roles": ["ADMIN"] → authority "ROLE_ADMIN", usata da hasRole('ADMIN'). */
    private JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter ruoli = new JwtGrantedAuthoritiesConverter();
        ruoli.setAuthoritiesClaimName(JwtClaims.ROLES);
        ruoli.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(ruoli);
        return converter;
    }

    /** BCrypt, con prefisso {bcrypt} per poter cambiare algoritmo in futuro. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
