package it.plannershop.platform;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.plannershop.auth.RefreshTokenRepository;
import it.plannershop.common.ApiException;
import it.plannershop.platform.PlatformDtos.ModificaTenantRequest;
import it.plannershop.platform.PlatformDtos.NuovoTenantRequest;
import it.plannershop.platform.PlatformDtos.TenantDto;
import it.plannershop.security.CurrentUser;
import it.plannershop.tenant.Tenant;
import it.plannershop.tenant.TenantRepository;
import it.plannershop.user.AppUser;
import it.plannershop.user.AppUserRepository;
import it.plannershop.user.Membership;
import it.plannershop.user.MembershipRepository;
import it.plannershop.user.Ruolo;

/** Gestione delle attività da parte del SUPERADMIN. */
@Service
public class PlatformTenantService {

    private final TenantRepository tenants;
    private final AppUserRepository users;
    private final MembershipRepository memberships;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;

    PlatformTenantService(TenantRepository tenants, AppUserRepository users, MembershipRepository memberships,
            RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder) {
        this.tenants = tenants;
        this.users = users;
        this.memberships = memberships;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<TenantDto> elenco() {
        return tenants.findAllByOrderByNomeAsc().stream().map(this::dto).toList();
    }

    /**
     * Crea un'attività e il suo primo amministratore. Se l'email appartiene già
     * a un utente, quell'utente diventa amministratore anche della nuova attività.
     */
    @Transactional
    public TenantDto crea(NuovoTenantRequest req) {
        Tenant tenant = tenants.save(new Tenant(req.nome().trim()));
        AppUser admin = users.findByEmailIgnoreCase(req.adminEmail().trim()).orElse(null);
        if (admin == null) {
            if (req.adminPassword() == null || req.adminPassword().isBlank()) {
                throw ApiException.richiestaNonValida("Inserisci una password provvisoria per il nuovo amministratore");
            }
            admin = new AppUser(req.adminEmail(), req.adminNome().trim(), passwordEncoder.encode(req.adminPassword()));
            admin.cambiaPassword(admin.getPasswordHash(), true);
            admin = users.save(admin);
        }
        memberships.save(new Membership(admin, tenant, Ruolo.ADMIN));
        return dto(tenant);
    }

    @Transactional
    public TenantDto modifica(Long id, ModificaTenantRequest req) {
        Tenant tenant = trova(id);
        tenant.setNome(req.nome().trim());
        tenant.setAttivo(req.attivo());
        if (!req.attivo()) {
            refreshTokens.revocaTuttiDiTenant(id);
        }
        return dto(tenant);
    }

    /** Eliminazione logica dell'attività e delle appartenenze; chiude le sessioni aperte. */
    @Transactional
    public void elimina(Long id) {
        Tenant tenant = trova(id);
        Long chi = CurrentUser.id();
        memberships.findByTenantId(id).forEach(m -> m.elimina(chi));
        tenant.elimina(chi);
        refreshTokens.revocaTuttiDiTenant(id);
    }

    private Tenant trova(Long id) {
        return tenants.findById(id).orElseThrow(() -> ApiException.nonTrovato("Attività non trovata"));
    }

    private TenantDto dto(Tenant t) {
        List<String> admin = memberships.findByTenantId(t.getId()).stream()
                .filter(m -> m.getRuolo() == Ruolo.ADMIN)
                .map(m -> m.getUser().getEmail())
                .sorted()
                .toList();
        return new TenantDto(t.getId(), t.getNome(), t.isAttivo(), t.getCreatedAt(), admin);
    }
}
