package it.plannershop.customer;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Queries see only the customers of the current tenant (@TenantId) that are not
 * deleted (@SQLRestriction): no filter has to be added by hand.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Search by first and last name (in both orders), phone or email.
     *
     * @param pattern see {@link it.plannershop.common.ListQuery#likePattern(String)}
     */
    @Query("""
            select c from Customer c
            where lower(concat(c.firstName, ' ', c.lastName)) like :pattern escape '\\'
               or lower(concat(c.lastName, ' ', c.firstName)) like :pattern escape '\\'
               or lower(c.phone) like :pattern escape '\\'
               or lower(c.email) like :pattern escape '\\'
            """)
    Page<Customer> search(String pattern, Pageable pageable);

    List<Customer> findByBirthMonthOrderByBirthDayAscLastNameAscFirstNameAsc(Short month);
}
