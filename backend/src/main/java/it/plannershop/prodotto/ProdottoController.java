package it.plannershop.prodotto;

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
import it.plannershop.prodotto.ProdottoDtos.ProdottoDto;
import it.plannershop.prodotto.ProdottoDtos.ProdottoRequest;
import jakarta.validation.Valid;

/**
 * Prodotti dell'attività corrente. Vedi SPECIFICA §5.3: ADMIN e OPERATORE
 * gestiscono il catalogo, eliminazione compresa.
 */
@RestController
@RequestMapping("/api/prodotti")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATORE')")
public class ProdottoController {

    private final ProdottoService service;

    ProdottoController(ProdottoService service) {
        this.service = service;
    }

    /** {@code GET /api/prodotti?q=shampoo&disponibili=true&page=0&size=20&sort=marca,asc} */
    @GetMapping
    Pagina<ProdottoDto> elenco(@RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean disponibili,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.elenco(q, disponibili, pageable);
    }

    @GetMapping("/{id}")
    ProdottoDto dettaglio(@PathVariable Long id) {
        return service.dettaglio(id);
    }

    @PostMapping
    ResponseEntity<ProdottoDto> crea(@Valid @RequestBody ProdottoRequest body) {
        ProdottoDto creato = service.crea(body);
        return ResponseEntity.created(URI.create("/api/prodotti/" + creato.id())).body(creato);
    }

    @PutMapping("/{id}")
    ProdottoDto modifica(@PathVariable Long id, @Valid @RequestBody ProdottoRequest body) {
        return service.modifica(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void elimina(@PathVariable Long id) {
        service.elimina(id);
    }
}
