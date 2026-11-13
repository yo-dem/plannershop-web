package it.plannershop.product;

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
import it.plannershop.product.ProductDtos.ProductDto;
import it.plannershop.product.ProductDtos.ProductRequest;
import jakarta.validation.Valid;

/**
 * Products of the current tenant. See SPECIFICA §5.3: ADMIN and OPERATOR manage
 * the catalog, deletion included.
 */
@RestController
@RequestMapping("/api/products")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
public class ProductController {

    private final ProductService service;

    ProductController(ProductService service) {
        this.service = service;
    }

    /** {@code GET /api/products?q=shampoo&available=true&page=0&size=20&sort=brand,asc} */
    @GetMapping
    PageResponse<ProductDto> list(@RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean available,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.list(q, available, pageable);
    }

    @GetMapping("/{id}")
    ProductDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    ResponseEntity<ProductDto> create(@Valid @RequestBody ProductRequest body) {
        ProductDto created = service.create(body);
        return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    ProductDto update(@PathVariable Long id, @Valid @RequestBody ProductRequest body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
