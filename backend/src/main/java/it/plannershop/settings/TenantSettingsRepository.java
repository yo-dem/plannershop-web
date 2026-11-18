package it.plannershop.settings;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** At most one row: the settings of the current tenant (filtered by @TenantId). */
public interface TenantSettingsRepository extends JpaRepository<TenantSettings, Long> {

    Optional<TenantSettings> findFirstBy();
}
