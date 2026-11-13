package it.plannershop.customer;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import it.plannershop.common.PageResponse;
import it.plannershop.customer.CustomerDtos.CustomerDto;
import it.plannershop.customer.CustomerDtos.CustomerRequest;
import jakarta.validation.Valid;

/**
 * Customers of the current tenant. See SPECIFICA §5.3: ADMIN and OPERATOR can
 * view, create and update; only the ADMIN deletes.
 */
@RestController
@RequestMapping("/api/customers")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
public class CustomerController {

    private final CustomerService service;

    CustomerController(CustomerService service) {
        this.service = service;
    }

    /** {@code GET /api/customers?q=rossi&page=0&size=20&sort=lastName,asc} */
    @GetMapping
    PageResponse<CustomerDto> list(@RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.list(q, pageable);
    }

    /** {@code GET /api/customers/birthdays?month=3}; without a month, the current one. */
    @GetMapping("/birthdays")
    List<CustomerDto> birthdays(@RequestParam(required = false) Integer month) {
        return service.birthdays(month);
    }

    @GetMapping("/{id}")
    CustomerDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    ResponseEntity<CustomerDto> create(@Valid @RequestBody CustomerRequest body) {
        CustomerDto created = service.create(body);
        return ResponseEntity.created(URI.create("/api/customers/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    CustomerDto update(@PathVariable Long id, @Valid @RequestBody CustomerRequest body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
