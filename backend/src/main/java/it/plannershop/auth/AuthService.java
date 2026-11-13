package it.plannershop.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.plannershop.auth.AuthDtos.CurrentUserDto;
import it.plannershop.auth.AuthDtos.Session;
import it.plannershop.auth.AuthDtos.TenantOption;
import it.plannershop.common.ApiException;
import it.plannershop.common.AppProperties;
import it.plannershop.security.LoginAttemptLimiter;
import it.plannershop.security.SecurityProperties;
import it.plannershop.tenant.Tenant;
import it.plannershop.user.AppUser;
import it.plannershop.user.AppUserRepository;
import it.plannershop.user.Membership;
import it.plannershop.user.MembershipRepository;

/** Sign-in, session renewal, sign-out and password change. */
@Service
public class AuthService {

    private final AppUserRepository users;
    private final MembershipRepository memberships;
    private final RefreshTokenRepository refreshTokens;
    private final TokenService tokens;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptLimiter limiter;
    private final SecurityProperties props;
    private final AppProperties app;
    /** Dummy hash: checked even if the user does not exist, so response times do not reveal it. */
    private final String dummyHash;

    AuthService(AppUserRepository users, MembershipRepository memberships,
            RefreshTokenRepository refreshTokens, TokenService tokens, PasswordEncoder passwordEncoder,
            LoginAttemptLimiter limiter, SecurityProperties props, AppProperties app) {
        this.users = users;
        this.memberships = memberships;
        this.refreshTokens = refreshTokens;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.limiter = limiter;
        this.props = props;
        this.app = app;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-constant-time");
    }

    /** Result of the sign-in: a session, or the list of tenants to choose from. */
    sealed interface LoginResult permits SignedIn, ChoiceRequired {
    }

    record SignedIn(Session session) implements LoginResult {
    }

    record ChoiceRequired(List<TenantOption> options) implements LoginResult {
    }

    @Transactional
    public LoginResult login(String email, String password, Long tenantId, String ip, String userAgent) {
        limiter.check(ip, email);

        AppUser user = users.findByEmailIgnoreCase(email.trim()).orElse(null);
        boolean passwordOk = passwordEncoder.matches(password, user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !passwordOk || !user.isActive()) {
            limiter.recordFailure(ip, email);
            throw ApiException.unauthorized("auth.invalidCredentials");
        }
        limiter.recordSuccess(email);

        Membership chosen;
        if (tenantId != null) {
            chosen = memberships.findValid(user.getId(), tenantId)
                    .orElseThrow(() -> ApiException.forbidden("auth.noAccessToTenant"));
        } else {
            List<Membership> valid = memberships.findValidByUser(user.getId());
            if (valid.size() > 1) {
                return new ChoiceRequired(valid.stream()
                        .map(m -> new TenantOption(m.getTenant().getId(), m.getTenant().getName()))
                        .toList());
            }
            if (valid.isEmpty() && !hasPlatformAccess(user)) {
                throw ApiException.forbidden("auth.noActiveTenant");
            }
            chosen = valid.isEmpty() ? null : valid.getFirst();
        }
        return new SignedIn(openSession(user, chosen, userAgent));
    }

    /**
     * Renews the session with a refresh token. The token used is revoked and
     * replaced (rotation). If an already revoked token arrives, someone may have
     * stolen it: all the user's sessions are revoked for safety.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public Session refresh(String refreshToken, String userAgent) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw ApiException.unauthorized("auth.sessionExpired");
        }
        RefreshToken stored = refreshTokens.findByTokenHash(TokenService.hash(refreshToken))
                .orElseThrow(() -> ApiException.unauthorized("auth.sessionExpired"));
        if (stored.isRevoked()) {
            refreshTokens.revokeAllOfUser(stored.getUserId());
            throw ApiException.unauthorized("auth.sessionRevoked");
        }
        if (stored.isExpired(Instant.now())) {
            throw ApiException.unauthorized("auth.sessionExpired");
        }
        stored.revoke();

        AppUser user = users.findById(stored.getUserId())
                .filter(AppUser::isActive)
                .orElseThrow(() -> ApiException.unauthorized("auth.userInactive"));
        Membership membership = null;
        if (stored.getTenantId() != null) {
            // The role is read again: if it has changed meanwhile, the new token reflects it.
            membership = memberships.findValid(user.getId(), stored.getTenantId())
                    .orElseThrow(() -> ApiException.unauthorized("auth.noLongerAccess"));
        } else if (!hasPlatformAccess(user)) {
            throw ApiException.unauthorized("auth.sessionRevoked");
        }
        return openSession(user, membership, userAgent);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokens.findByTokenHash(TokenService.hash(refreshToken)).ifPresent(RefreshToken::revoke);
        }
    }

    @Transactional(readOnly = true)
    public CurrentUserDto currentUser(Long userId, Long tenantId) {
        AppUser user = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("auth.userNotFound"));
        Membership membership = tenantId == null ? null
                : memberships.findValid(userId, tenantId)
                        .orElseThrow(() -> ApiException.unauthorized("auth.noLongerAccess"));
        return userDto(user, membership);
    }

    @Transactional
    public void changePassword(Long userId, String current, String newPassword) {
        AppUser user = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("auth.userNotFound"));
        if (!passwordEncoder.matches(current, user.getPasswordHash())) {
            throw ApiException.badRequest("auth.currentPasswordWrong");
        }
        if (current.equals(newPassword)) {
            throw ApiException.badRequest("auth.newPasswordSame");
        }
        user.changePassword(passwordEncoder.encode(newPassword), false);
    }

    private Session openSession(AppUser user, Membership membership, String userAgent) {
        String refresh = tokens.newRefreshToken();
        Long tenantId = membership != null ? membership.getTenant().getId() : null;
        refreshTokens.save(new RefreshToken(user.getId(), tenantId, TokenService.hash(refresh), userAgent,
                Instant.now().plus(props.refreshTokenTtl())));
        return new Session(tokens.createAccessToken(user, membership), tokens.accessTokenTtlSeconds(), refresh,
                userDto(user, membership));
    }

    /** Only in CLOUD mode can the SUPERADMIN work without a tenant. */
    private boolean hasPlatformAccess(AppUser user) {
        return user.isSuperadmin() && !app.isLocal();
    }

    private CurrentUserDto userDto(AppUser user, Membership membership) {
        String role = membership != null ? membership.getRole().name() : "SUPERADMIN";
        TenantOption tenant = null;
        if (membership != null) {
            Tenant t = membership.getTenant();
            tenant = new TenantOption(t.getId(), t.getName());
        }
        return new CurrentUserDto(user.getId(), user.getEmail(), user.getName(), role, hasPlatformAccess(user), tenant,
                user.isMustChangePassword());
    }

    Duration refreshTtl() {
        return props.refreshTokenTtl();
    }
}
