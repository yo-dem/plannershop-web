package it.plannershop.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.plannershop.auth.RefreshTokenRepository;
import it.plannershop.common.ApiException;
import it.plannershop.common.RandomPassword;

/**
 * Emergency password reset, run by whoever has access to the server (see
 * {@link PasswordResetRunner}). Mostly needed in LOCAL mode, where there is no
 * SUPERADMIN who could step in.
 */
@Service
public class PasswordReset {

    private final AppUserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;

    PasswordReset(AppUserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Assigns a new temporary password (to be changed at the first sign-in),
     * reactivates the user and closes all their open sessions.
     *
     * @return the generated temporary password
     */
    @Transactional
    public String reset(String email) {
        AppUser user = users.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> ApiException.notFound("error.notFound.userWithEmail", email));
        String pwd = RandomPassword.generate();
        user.changePassword(passwordEncoder.encode(pwd), true);
        user.setActive(true);
        refreshTokens.revokeAllOfUser(user.getId());
        return pwd;
    }
}
