package it.plannershop.serviceitem;

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
import it.plannershop.serviceitem.ServiceItemDtos.ServiceItemDto;
import it.plannershop.serviceitem.ServiceItemDtos.ServiceItemRequest;
import jakarta.validation.Valid;

/**
 * Services of the current tenant. See SPECIFICA §5.3: ADMIN and OPERATOR manage
 * the catalog, deletion included.
 */
@RestController
@RequestMapping("/api/services")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
public class ServiceItemController {

    private final ServiceItemService service;

    ServiceItemController(ServiceItemService service) {
        this.service = service;
    }

    /** {@code GET /api/services?q=haircut&page=0&size=20&sort=name,asc} */
    @GetMapping
    PageResponse<ServiceItemDto> list(@RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.list(q, pageable);
    }

    @GetMapping("/{id}")
    ServiceItemDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    ResponseEntity<ServiceItemDto> create(@Valid @RequestBody ServiceItemRequest body) {
        ServiceItemDto created = service.create(body);
        return ResponseEntity.created(URI.create("/api/services/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    ServiceItemDto update(@PathVariable Long id, @Valid @RequestBody ServiceItemRequest body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
