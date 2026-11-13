package it.plannershop.supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Only suppliers of the current tenant (@TenantId) that are not deleted (@SQLRestriction). */
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    /**
     * Search on name, address, phone, email, IBAN and notes.
     *
     * @param pattern     see {@link it.plannershop.common.ListQuery#likePattern(String)}
     * @param ibanPattern like pattern, but without spaces (the IBAN is stored without spaces)
     */
    @Query("""
            select s from Supplier s
            where lower(s.name) like :pattern escape '\\'
               or lower(s.address) like :pattern escape '\\'
               or lower(s.phone) like :pattern escape '\\'
               or lower(s.email) like :pattern escape '\\'
               or lower(s.iban) like :ibanPattern escape '\\'
               or lower(s.notes) like :pattern escape '\\'
            """)
    Page<Supplier> search(String pattern, String ibanPattern, Pageable pageable);
}
