package it.plannershop.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.plannershop.auth.RefreshTokenRepository;
import it.plannershop.common.ApiException;
import it.plannershop.common.PasswordCasuale;

/**
 * Ripristino d'emergenza della password di un utente, eseguito da chi ha
 * accesso al server (vedi {@link RipristinoPasswordRunner}). Serve soprattutto
 * in modalità LOCALE, dove non c'è un SUPERADMIN che possa intervenire.
 */
@Service
public class RipristinoPassword {

    private final AppUserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;

    RipristinoPassword(AppUserRepository users, RefreshTokenRepository refreshTokens,
            PasswordEncoder passwordEncoder) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Assegna una nuova password provvisoria (da cambiare al primo accesso),
     * riattiva l'utente e chiude tutte le sue sessioni aperte.
     *
     * @return la password provvisoria generata
     */
    @Transactional
    public String ripristina(String email) {
        AppUser user = users.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> ApiException.nonTrovato("Nessun utente con email " + email));
        String pwd = PasswordCasuale.genera();
        user.cambiaPassword(passwordEncoder.encode(pwd), true);
        user.setAttivo(true);
        refreshTokens.revocaTuttiDiUtente(user.getId());
        return pwd;
    }
}
