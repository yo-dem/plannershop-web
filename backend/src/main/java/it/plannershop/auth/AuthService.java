package it.plannershop.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.plannershop.auth.AuthDtos.Sessione;
import it.plannershop.auth.AuthDtos.TenantOpzione;
import it.plannershop.auth.AuthDtos.UtenteCorrente;
import it.plannershop.common.ApiException;
import it.plannershop.security.LoginAttemptLimiter;
import it.plannershop.security.SecurityProperties;
import it.plannershop.tenant.Tenant;
import it.plannershop.user.AppUser;
import it.plannershop.user.AppUserRepository;
import it.plannershop.user.Membership;
import it.plannershop.user.MembershipRepository;

/** Login, rinnovo della sessione, logout e cambio password. */
@Service
public class AuthService {

    private static final String CREDENZIALI_NON_VALIDE = "Email o password non corretti";

    private final AppUserRepository users;
    private final MembershipRepository memberships;
    private final RefreshTokenRepository refreshTokens;
    private final TokenService tokens;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptLimiter limiter;
    private final SecurityProperties props;
    /** Hash fittizio: si verifica anche se l'utente non esiste, per non rivelarlo dai tempi di risposta. */
    private final String hashFittizio;

    AuthService(AppUserRepository users, MembershipRepository memberships,
            RefreshTokenRepository refreshTokens, TokenService tokens, PasswordEncoder passwordEncoder,
            LoginAttemptLimiter limiter, SecurityProperties props) {
        this.users = users;
        this.memberships = memberships;
        this.refreshTokens = refreshTokens;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.limiter = limiter;
        this.props = props;
        this.hashFittizio = passwordEncoder.encode("password-fittizia-per-tempi-costanti");
    }

    /** Esito del login: una sessione, oppure l'elenco delle attività tra cui scegliere. */
    sealed interface EsitoLogin permits Accesso, SceltaRichiesta {
    }

    record Accesso(Sessione sessione) implements EsitoLogin {
    }

    record SceltaRichiesta(List<TenantOpzione> opzioni) implements EsitoLogin {
    }

    @Transactional
    public EsitoLogin login(String email, String password, Long tenantId, String ip, String userAgent) {
        limiter.verifica(ip, email);

        AppUser user = users.findByEmailIgnoreCase(email.trim()).orElse(null);
        boolean passwordOk = passwordEncoder.matches(password, user != null ? user.getPasswordHash() : hashFittizio);
        if (user == null || !passwordOk || !user.isAttivo()) {
            limiter.registraFallimento(ip, email);
            throw ApiException.nonAutenticato(CREDENZIALI_NON_VALIDE);
        }
        limiter.registraSuccesso(email);

        Membership scelta;
        if (tenantId != null) {
            scelta = memberships.findValida(user.getId(), tenantId)
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Non hai accesso a questa attività"));
        } else {
            List<Membership> valide = memberships.findValideByUser(user.getId());
            if (valide.size() > 1) {
                return new SceltaRichiesta(valide.stream()
                        .map(m -> new TenantOpzione(m.getTenant().getId(), m.getTenant().getNome()))
                        .toList());
            }
            if (valide.isEmpty() && !user.isSuperadmin()) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Il tuo utente non è associato a nessuna attività attiva");
            }
            scelta = valide.isEmpty() ? null : valide.getFirst();
        }
        return new Accesso(apriSessione(user, scelta, userAgent));
    }

    /**
     * Rinnova la sessione con un refresh token. Il token usato viene revocato e
     * sostituito (rotazione). Se arriva un token già revocato, qualcuno potrebbe
     * averlo rubato: per sicurezza si revocano tutte le sessioni dell'utente.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public Sessione refresh(String refreshToken, String userAgent) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw ApiException.nonAutenticato("Sessione scaduta");
        }
        RefreshToken salvato = refreshTokens.findByTokenHash(TokenService.hash(refreshToken))
                .orElseThrow(() -> ApiException.nonAutenticato("Sessione scaduta"));
        if (salvato.isRevocato()) {
            refreshTokens.revocaTuttiDiUtente(salvato.getUserId());
            throw ApiException.nonAutenticato("Sessione non più valida, accedi di nuovo");
        }
        if (salvato.isScaduto(Instant.now())) {
            throw ApiException.nonAutenticato("Sessione scaduta");
        }
        salvato.revoca();

        AppUser user = users.findById(salvato.getUserId())
                .filter(AppUser::isAttivo)
                .orElseThrow(() -> ApiException.nonAutenticato("Utente non più attivo"));
        Membership membership = null;
        if (salvato.getTenantId() != null) {
            // Si rilegge il ruolo: se nel frattempo è cambiato, il nuovo token lo riflette.
            membership = memberships.findValida(user.getId(), salvato.getTenantId())
                    .orElseThrow(() -> ApiException.nonAutenticato("Non hai più accesso a questa attività"));
        } else if (!user.isSuperadmin()) {
            throw ApiException.nonAutenticato("Sessione non più valida");
        }
        return apriSessione(user, membership, userAgent);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokens.findByTokenHash(TokenService.hash(refreshToken)).ifPresent(RefreshToken::revoca);
        }
    }

    @Transactional(readOnly = true)
    public UtenteCorrente utenteCorrente(Long userId, Long tenantId) {
        AppUser user = users.findById(userId).orElseThrow(() -> ApiException.nonAutenticato("Utente non trovato"));
        Membership membership = tenantId == null ? null
                : memberships.findValida(userId, tenantId)
                        .orElseThrow(() -> ApiException.nonAutenticato("Non hai più accesso a questa attività"));
        return utente(user, membership);
    }

    @Transactional
    public void cambiaPassword(Long userId, String attuale, String nuova) {
        AppUser user = users.findById(userId).orElseThrow(() -> ApiException.nonAutenticato("Utente non trovato"));
        if (!passwordEncoder.matches(attuale, user.getPasswordHash())) {
            throw ApiException.richiestaNonValida("La password attuale non è corretta");
        }
        if (attuale.equals(nuova)) {
            throw ApiException.richiestaNonValida("La nuova password deve essere diversa da quella attuale");
        }
        user.cambiaPassword(passwordEncoder.encode(nuova), false);
    }

    private Sessione apriSessione(AppUser user, Membership membership, String userAgent) {
        String refresh = tokens.nuovoRefreshToken();
        Long tenantId = membership != null ? membership.getTenant().getId() : null;
        refreshTokens.save(new RefreshToken(user.getId(), tenantId, TokenService.hash(refresh), userAgent,
                Instant.now().plus(props.refreshTokenTtl())));
        return new Sessione(tokens.creaAccessToken(user, membership), tokens.durataAccessTokenSecondi(), refresh,
                utente(user, membership));
    }

    private static UtenteCorrente utente(AppUser user, Membership membership) {
        String ruolo = membership != null ? membership.getRuolo().name() : "SUPERADMIN";
        TenantOpzione tenant = null;
        if (membership != null) {
            Tenant t = membership.getTenant();
            tenant = new TenantOpzione(t.getId(), t.getNome());
        }
        return new UtenteCorrente(user.getId(), user.getEmail(), user.getNome(), ruolo, user.isSuperadmin(), tenant,
                user.isDeveCambiarePassword());
    }

    Duration durataRefresh() {
        return props.refreshTokenTtl();
    }
}
