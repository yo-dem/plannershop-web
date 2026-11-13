package it.plannershop.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken r set r.revokedAt = current_timestamp where r.userId = :userId and r.revokedAt is null")
    int revokeAllOfUser(Long userId);

    @Modifying
    @Query("update RefreshToken r set r.revokedAt = current_timestamp where r.tenantId = :tenantId and r.revokedAt is null")
    int revokeAllOfTenant(Long tenantId);
}
