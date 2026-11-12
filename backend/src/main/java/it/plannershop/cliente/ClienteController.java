package it.plannershop.cliente;

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

import it.plannershop.cliente.ClienteDtos.ClienteDto;
import it.plannershop.cliente.ClienteDtos.ClienteRequest;
import it.plannershop.common.Pagina;
import jakarta.validation.Valid;

/**
 * Clienti dell'attività corrente. Vedi SPECIFICA §5.3: ADMIN e OPERATORE
 * vedono, inseriscono e modificano; solo l'ADMIN elimina.
 */
@RestController
@RequestMapping("/api/clienti")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATORE')")
public class ClienteController {

    private final ClienteService service;

    ClienteController(ClienteService service) {
        this.service = service;
    }

    /** {@code GET /api/clienti?q=rossi&page=0&size=20&sort=cognome,asc} */
    @GetMapping
    Pagina<ClienteDto> elenco(@RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.elenco(q, pageable);
    }

    /** {@code GET /api/clienti/compleanni?mese=3}; senza mese, il mese corrente. */
    @GetMapping("/compleanni")
    List<ClienteDto> compleanni(@RequestParam(required = false) Integer mese) {
        return service.compleanni(mese);
    }

    @GetMapping("/{id}")
    ClienteDto dettaglio(@PathVariable Long id) {
        return service.dettaglio(id);
    }

    @PostMapping
    ResponseEntity<ClienteDto> crea(@Valid @RequestBody ClienteRequest body) {
        ClienteDto creato = service.crea(body);
        return ResponseEntity.created(URI.create("/api/clienti/" + creato.id())).body(creato);
    }

    @PutMapping("/{id}")
    ClienteDto modifica(@PathVariable Long id, @Valid @RequestBody ClienteRequest body) {
        return service.modifica(id, body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void elimina(@PathVariable Long id) {
        service.elimina(id);
    }
}
