package it.plannershop.platform;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.plannershop.auth.RefreshTokenRepository;
import it.plannershop.common.ApiException;
import it.plannershop.platform.PlatformDtos.NewTenantRequest;
import it.plannershop.platform.PlatformDtos.TenantDto;
import it.plannershop.platform.PlatformDtos.UpdateTenantRequest;
import it.plannershop.security.CurrentUser;
import it.plannershop.tenant.Tenant;
import it.plannershop.tenant.TenantRepository;
import it.plannershop.user.AppUser;
import it.plannershop.user.AppUserRepository;
import it.plannershop.user.Membership;
import it.plannershop.user.MembershipRepository;
import it.plannershop.user.Role;

/** Tenant management by the SUPERADMIN (CLOUD mode only). */
@Service
@ConditionalOnProperty(name = "app.mode", havingValue = "cloud", matchIfMissing = true)
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
    public List<TenantDto> list() {
        return tenants.findAllByOrderByNameAsc().stream().map(this::dto).toList();
    }

    /**
     * Creates a tenant and its first administrator. If the email already belongs
     * to a user, that user becomes administrator of the new tenant as well.
     */
    @Transactional
    public TenantDto create(NewTenantRequest req) {
        Tenant tenant = tenants.save(new Tenant(req.name().trim()));
        AppUser admin = users.findByEmailIgnoreCase(req.adminEmail().trim()).orElse(null);
        if (admin == null) {
            if (req.adminPassword() == null || req.adminPassword().isBlank()) {
                throw ApiException.badRequest("platform.adminPasswordRequired");
            }
            admin = new AppUser(req.adminEmail(), req.adminName().trim(), passwordEncoder.encode(req.adminPassword()));
            admin.changePassword(admin.getPasswordHash(), true);
            admin = users.save(admin);
        }
        memberships.save(new Membership(admin, tenant, Role.ADMIN));
        return dto(tenant);
    }

    @Transactional
    public TenantDto update(Long id, UpdateTenantRequest req) {
        Tenant tenant = find(id);
        tenant.setName(req.name().trim());
        tenant.setActive(req.active());
        if (!req.active()) {
            refreshTokens.revokeAllOfTenant(id);
        }
        return dto(tenant);
    }

    /** Soft delete of the tenant and its memberships; closes the open sessions. */
    @Transactional
    public void delete(Long id) {
        Tenant tenant = find(id);
        Long by = CurrentUser.id();
        memberships.findByTenantId(id).forEach(m -> m.delete(by));
        tenant.delete(by);
        refreshTokens.revokeAllOfTenant(id);
    }

    private Tenant find(Long id) {
        return tenants.findById(id).orElseThrow(() -> ApiException.notFound("error.notFound.tenant"));
    }

    private TenantDto dto(Tenant t) {
        List<String> admins = memberships.findByTenantId(t.getId()).stream()
                .filter(m -> m.getRole() == Role.ADMIN)
                .map(m -> m.getUser().getEmail())
                .sorted()
                .toList();
        return new TenantDto(t.getId(), t.getName(), t.isActive(), t.getCreatedAt(), admins);
    }
}
