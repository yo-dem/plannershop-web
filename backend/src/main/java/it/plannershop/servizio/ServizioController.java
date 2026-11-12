package it.plannershop.servizio;

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
import it.plannershop.servizio.ServizioDtos.ServizioDto;
import it.plannershop.servizio.ServizioDtos.ServizioRequest;
import jakarta.validation.Valid;

/**
 * Servizi dell'attività corrente. Vedi SPECIFICA §5.3: ADMIN e OPERATORE
 * gestiscono il catalogo, eliminazione compresa.
 */
@RestController
@RequestMapping("/api/servizi")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATORE')")
public class ServizioController {

    private final ServizioService service;

    ServizioController(ServizioService service) {
        this.service = service;
    }

    /** {@code GET /api/servizi?q=piega&page=0&size=20&sort=nome,asc} */
    @GetMapping
    Pagina<ServizioDto> elenco(@RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.elenco(q, pageable);
    }

    @GetMapping("/{id}")
    ServizioDto dettaglio(@PathVariable Long id) {
        return service.dettaglio(id);
    }

    @PostMapping
    ResponseEntity<ServizioDto> crea(@Valid @RequestBody ServizioRequest body) {
        ServizioDto creato = service.crea(body);
        return ResponseEntity.created(URI.create("/api/servizi/" + creato.id())).body(creato);
    }

    @PutMapping("/{id}")
    ServizioDto modifica(@PathVariable Long id, @Valid @RequestBody ServizioRequest body) {
        return service.modifica(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void elimina(@PathVariable Long id) {
        service.elimina(id);
    }
}
