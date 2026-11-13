package it.plannershop.customer;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.plannershop.common.ApiException;
import it.plannershop.common.ListQuery;
import it.plannershop.common.PageResponse;
import it.plannershop.customer.CustomerDtos.CustomerDto;
import it.plannershop.customer.CustomerDtos.CustomerRequest;
import it.plannershop.security.CurrentUser;
import it.plannershop.tenant.TenantClock;

/** Customers of the current tenant. */
@Service
public class CustomerService {

    /** Allowed sort keys: name used by the frontend → entity properties. */
    private static final Map<String, List<String>> SORTS = Map.of(
            "firstName", List.of("firstName"),
            "lastName", List.of("lastName"),
            "birthday", List.of("birthMonth", "birthDay"),
            "createdAt", List.of("createdAt"),
            "updatedAt", List.of("updatedAt"));

    /** Appended to every sort: last name, first name, id (stable order across pages). */
    private static final List<Sort.Order> DEFAULT_ORDER = List.of(
            Sort.Order.asc("lastName").ignoreCase(), Sort.Order.asc("firstName").ignoreCase(), Sort.Order.asc("id"));

    private final CustomerRepository customers;
    private final TenantClock clock;

    CustomerService(CustomerRepository customers, TenantClock clock) {
        this.customers = customers;
        this.clock = clock;
    }

    /**
     * Paged list, with optional search on first and last name (in any order),
     * phone and email. Without a sort: by last and first name.
     */
    @Transactional(readOnly = true)
    public PageResponse<CustomerDto> list(String q, Pageable request) {
        Pageable pageable = ListQuery.pageable(request, SORTS, Set.of("firstName", "lastName"), DEFAULT_ORDER);
        return PageResponse.of(customers.search(ListQuery.likePattern(q), pageable), CustomerDto::of);
    }

    @Transactional(readOnly = true)
    public CustomerDto get(Long id) {
        return CustomerDto.of(find(id));
    }

    @Transactional
    public CustomerDto create(CustomerRequest req) {
        Customer c = new Customer(null, null);
        apply(c, req);
        return CustomerDto.of(customers.saveAndFlush(c));
    }

    @Transactional
    public CustomerDto update(Long id, CustomerRequest req) {
        Customer c = find(id);
        if (req.version() == null) {
            throw ApiException.badRequest("error.versionMissing");
        }
        if (req.version() != c.getVersion()) {
            throw ApiException.conflict("error.conflict");
        }
        apply(c, req);
        return CustomerDto.of(customers.saveAndFlush(c));
    }

    /** Soft delete: the customer disappears from the lists but stays in the database. */
    @Transactional
    public void delete(Long id) {
        find(id).delete(CurrentUser.id());
    }

    /**
     * Customers with a birthday in the given month, sorted by day.
     * Without a month: the current month in the tenant's time zone.
     */
    @Transactional(readOnly = true)
    public List<CustomerDto> birthdays(Integer month) {
        int m = month != null ? month : clock.today().getMonthValue();
        if (m < 1 || m > 12) {
            throw ApiException.badRequest("error.invalidMonth");
        }
        return customers.findByBirthMonthOrderByBirthDayAscLastNameAscFirstNameAsc((short) m).stream()
                .map(CustomerDto::of)
                .toList();
    }

    private Customer find(Long id) {
        return customers.findById(id).orElseThrow(() -> ApiException.notFound("error.notFound.customer"));
    }

    /** Text is already cleaned (see {@link CustomerRequest}). */
    private static void apply(Customer c, CustomerRequest req) {
        c.setFirstName(req.firstName());
        c.setLastName(req.lastName());
        c.setBirthday(req.birthDay(), req.birthMonth());
        c.setPhone(req.phone());
        c.setEmail(req.email() != null ? req.email().toLowerCase() : null);
        c.setAddress(req.address());
        c.setNotes(req.notes());
    }
}
