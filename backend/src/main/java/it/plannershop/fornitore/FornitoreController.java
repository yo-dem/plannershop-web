package it.plannershop.fornitore;

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

import it.plannershop.common.Pagina;
import it.plannershop.fornitore.FornitoreDtos.FornitoreDto;
import it.plannershop.fornitore.FornitoreDtos.FornitoreRequest;
import jakarta.validation.Valid;

/**
 * Fornitori dell'attività corrente. Vedi SPECIFICA §5.3: ADMIN e OPERATORE
 * li gestiscono, eliminazione compresa.
 */
@RestController
@RequestMapping("/api/fornitori")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATORE')")
public class FornitoreController {

    private final FornitoreService service;

    FornitoreController(FornitoreService service) {
        this.service = service;
    }

    /** {@code GET /api/fornitori?q=srl&page=0&size=20&sort=nome,asc} */
    @GetMapping
    Pagina<FornitoreDto> elenco(@RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.elenco(q, pageable);
    }

    @GetMapping("/{id}")
    FornitoreDto dettaglio(@PathVariable Long id) {
        return service.dettaglio(id);
    }

    @PostMapping
    ResponseEntity<FornitoreDto> crea(@Valid @RequestBody FornitoreRequest body) {
        FornitoreDto creato = service.crea(body);
        return ResponseEntity.created(URI.create("/api/fornitori/" + creato.id())).body(creato);
    }

    @PutMapping("/{id}")
    FornitoreDto modifica(@PathVariable Long id, @Valid @RequestBody FornitoreRequest body) {
        return service.modifica(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void elimina(@PathVariable Long id) {
        service.elimina(id);
    }
}
