package it.plannershop.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    /** Appartenenze valide: attività attiva e non eliminata. */
    @Query("""
            select m from Membership m join fetch m.tenant t
            where m.user.id = :userId and t.attivo = true and t.deletedAt is null
            order by t.nome""")
    List<Membership> findValideByUser(Long userId);

    @Query("""
            select m from Membership m join fetch m.tenant t
            where m.user.id = :userId and t.id = :tenantId and t.attivo = true and t.deletedAt is null""")
    Optional<Membership> findValida(Long userId, Long tenantId);

    List<Membership> findByTenantId(Long tenantId);

    boolean existsByUserIdAndTenantId(Long userId, Long tenantId);
}
