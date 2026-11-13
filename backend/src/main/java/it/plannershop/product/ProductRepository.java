package it.plannershop.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Only products of the current tenant (@TenantId) that are not deleted (@SQLRestriction). */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Search on brand and description (in both orders) and notes.
     *
     * @param pattern       see {@link it.plannershop.common.ListQuery#likePattern(String)}
     * @param availableOnly true = only products with quantity greater than zero
     */
    @Query("""
            select p from Product p
            where (lower(concat(p.brand, ' ', p.description)) like :pattern escape '\\'
                   or lower(concat(p.description, ' ', p.brand)) like :pattern escape '\\'
                   or lower(p.notes) like :pattern escape '\\')
              and (:availableOnly = false or p.quantity > 0)
            """)
    Page<Product> search(String pattern, boolean availableOnly, Pageable pageable);
}
