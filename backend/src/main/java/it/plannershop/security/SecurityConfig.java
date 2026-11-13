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
 * Access rules for the APIs.
 *
 * <ul>
 * <li>Every request (except login/refresh/logout, health and docs) must carry the
 * header {@code Authorization: Bearer <access token>}.</li>
 * <li>No server-side session (stateless): everything is in the token.</li>
 * <li>CSRF disabled: the APIs do not authenticate with cookies; the only cookie,
 * the refresh token, is {@code SameSite=Strict} and limited to {@code /api/auth}.</li>
 * <li>Role permissions are declared on methods with {@code @PreAuthorize}.</li>
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

    /** Claim "roles": ["ADMIN"] → authority "ROLE_ADMIN", used by hasRole('ADMIN'). */
    private JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(JwtClaims.ROLES);
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    /** BCrypt, with the {bcrypt} prefix so the algorithm can be changed in the future. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
