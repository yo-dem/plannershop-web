package it.plannershop.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    /** Valid memberships: tenant active and not deleted. */
    @Query("""
            select m from Membership m join fetch m.tenant t
            where m.user.id = :userId and t.active = true and t.deletedAt is null
            order by t.name""")
    List<Membership> findValidByUser(Long userId);

    @Query("""
            select m from Membership m join fetch m.tenant t
            where m.user.id = :userId and t.id = :tenantId and t.active = true and t.deletedAt is null""")
    Optional<Membership> findValid(Long userId, Long tenantId);

    List<Membership> findByTenantId(Long tenantId);

    boolean existsByUserIdAndTenantId(Long userId, Long tenantId);
}
