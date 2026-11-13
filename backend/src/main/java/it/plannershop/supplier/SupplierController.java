package it.plannershop.supplier;

import java.net.URI;

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
import it.plannershop.supplier.SupplierDtos.SupplierDto;
import it.plannershop.supplier.SupplierDtos.SupplierRequest;
import jakarta.validation.Valid;

/**
 * Suppliers of the current tenant. See SPECIFICA §5.3: ADMIN and OPERATOR
 * manage them, deletion included.
 */
@RestController
@RequestMapping("/api/suppliers")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
public class SupplierController {

    private final SupplierService service;

    SupplierController(SupplierService service) {
        this.service = service;
    }

    /** {@code GET /api/suppliers?q=srl&page=0&size=20&sort=name,asc} */
    @GetMapping
    PageResponse<SupplierDto> list(@RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.list(q, pageable);
    }

    @GetMapping("/{id}")
    SupplierDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    ResponseEntity<SupplierDto> create(@Valid @RequestBody SupplierRequest body) {
        SupplierDto created = service.create(body);
        return ResponseEntity.created(URI.create("/api/suppliers/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    SupplierDto update(@PathVariable Long id, @Valid @RequestBody SupplierRequest body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
