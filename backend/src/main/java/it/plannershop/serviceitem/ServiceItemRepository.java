package it.plannershop.serviceitem;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Only services of the current tenant (@TenantId) that are not deleted (@SQLRestriction). */
public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {

    /**
     * Search on name, description and notes.
     *
     * @param pattern see {@link it.plannershop.common.ListQuery#likePattern(String)}
     */
    @Query("""
            select s from ServiceItem s
            where lower(s.name) like :pattern escape '\\'
               or lower(s.description) like :pattern escape '\\'
               or lower(s.notes) like :pattern escape '\\'
            """)
    Page<ServiceItem> search(String pattern, Pageable pageable);
}
