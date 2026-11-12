package it.plannershop.prodotto;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.plannershop.common.ApiException;
import it.plannershop.common.Pagina;
import it.plannershop.common.PrezziArticolo;
import it.plannershop.common.Ricerca;
import it.plannershop.prodotto.ProdottoDtos.ProdottoDto;
import it.plannershop.prodotto.ProdottoDtos.ProdottoRequest;
import it.plannershop.security.CurrentUser;
import it.plannershop.tenant.OrologioAttivita;

/** Prodotti e magazzino dell'attività corrente. */
@Service
public class ProdottoService {

    /** Ordinamenti consentiti: nome usato dal frontend → proprietà dell'entità. */
    private static final Map<String, List<String>> ORDINAMENTI = Map.of(
            "marca", List.of("marca", "descrizione"),
            "descrizione", List.of("descrizione"),
            "quantita", List.of("quantita"),
            "prezzoVendita", List.of("prezzi.prezzoVendita"),
            "dataCarico", List.of("dataCarico"),
            "createdAt", List.of("createdAt"),
            "updatedAt", List.of("updatedAt"));

    private static final List<Sort.Order> PREDEFINITO = List.of(
            Sort.Order.asc("marca").ignoreCase(), Sort.Order.asc("descrizione").ignoreCase(), Sort.Order.asc("id"));

    private final ProdottoRepository prodotti;
    private final OrologioAttivita orologio;

    ProdottoService(ProdottoRepository prodotti, OrologioAttivita orologio) {
        this.prodotti = prodotti;
        this.orologio = orologio;
    }

    /**
     * Elenco paginato con ricerca facoltativa su marca, descrizione e note.
     * Senza ordinamento: per marca e descrizione.
     *
     * @param disponibili solo i prodotti con quantità maggiore di zero (per le vendite)
     */
    @Transactional(readOnly = true)
    public Pagina<ProdottoDto> elenco(String q, boolean disponibili, Pageable richiesta) {
        Pageable pageable = Ricerca.pagina(richiesta, ORDINAMENTI, Set.of("marca", "descrizione"), PREDEFINITO);
        return Pagina.di(prodotti.cerca(Ricerca.testoLike(q), disponibili, pageable), ProdottoDto::di);
    }

    @Transactional(readOnly = true)
    public ProdottoDto dettaglio(Long id) {
        return ProdottoDto.di(trova(id));
    }

    @Transactional
    public ProdottoDto crea(ProdottoRequest req) {
        Prodotto p = new Prodotto(null, null);
        applica(p, req);
        if (p.getDataCarico() == null) {
            p.setDataCarico(orologio.oggi());
        }
        return ProdottoDto.di(prodotti.saveAndFlush(p));
    }

    @Transactional
    public ProdottoDto modifica(Long id, ProdottoRequest req) {
        Prodotto p = trova(id);
        if (req.version() == null) {
            throw ApiException.richiestaNonValida("Versione del prodotto mancante");
        }
        if (req.version() != p.getVersion()) {
            throw ApiException.conflitto("Il prodotto è stato modificato da un altro utente. Ricarica e riprova.");
        }
        applica(p, req);
        return ProdottoDto.di(prodotti.saveAndFlush(p));
    }

    /** Eliminazione logica: il prodotto resta nel database (e nelle vendite passate). */
    @Transactional
    public void elimina(Long id) {
        trova(id).elimina(CurrentUser.id());
    }

    private Prodotto trova(Long id) {
        return prodotti.findById(id).orElseThrow(() -> ApiException.nonTrovato("Prodotto non trovato"));
    }

    /** I testi arrivano già puliti (vedi {@link ProdottoRequest}). */
    private static void applica(Prodotto p, ProdottoRequest req) {
        p.setDataCarico(req.dataCarico());
        p.setMarca(req.marca());
        p.setDescrizione(req.descrizione());
        p.setQuantita(req.quantita());
        p.setPrezzi(PrezziArticolo.di(req));
        p.setNote(req.note());
    }
}
