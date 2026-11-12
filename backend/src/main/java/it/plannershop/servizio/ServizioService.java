package it.plannershop.servizio;

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
import it.plannershop.servizio.ServizioDtos.ServizioDto;
import it.plannershop.servizio.ServizioDtos.ServizioRequest;
import it.plannershop.security.CurrentUser;

/** Servizi offerti dall'attività corrente. */
@Service
public class ServizioService {

    /** Ordinamenti consentiti: nome usato dal frontend → proprietà dell'entità. */
    private static final Map<String, List<String>> ORDINAMENTI = Map.of(
            "nome", List.of("nome"),
            "descrizione", List.of("descrizione"),
            "prezzoVendita", List.of("prezzi.prezzoVendita"),
            "createdAt", List.of("createdAt"),
            "updatedAt", List.of("updatedAt"));

    private static final List<Sort.Order> PREDEFINITO = List.of(
            Sort.Order.asc("nome").ignoreCase(), Sort.Order.asc("descrizione").ignoreCase(), Sort.Order.asc("id"));

    private final ServizioRepository servizi;

    ServizioService(ServizioRepository servizi) {
        this.servizi = servizi;
    }

    /**
     * Elenco paginato con ricerca facoltativa su nome, descrizione e note.
     * Senza ordinamento: per nome.
     */
    @Transactional(readOnly = true)
    public Pagina<ServizioDto> elenco(String q, Pageable richiesta) {
        Pageable pageable = Ricerca.pagina(richiesta, ORDINAMENTI, Set.of("nome", "descrizione"), PREDEFINITO);
        return Pagina.di(servizi.cerca(Ricerca.testoLike(q), pageable), ServizioDto::di);
    }

    @Transactional(readOnly = true)
    public ServizioDto dettaglio(Long id) {
        return ServizioDto.di(trova(id));
    }

    @Transactional
    public ServizioDto crea(ServizioRequest req) {
        Servizio s = new Servizio(null, null);
        applica(s, req);
        return ServizioDto.di(servizi.saveAndFlush(s));
    }

    @Transactional
    public ServizioDto modifica(Long id, ServizioRequest req) {
        Servizio s = trova(id);
        if (req.version() == null) {
            throw ApiException.richiestaNonValida("Versione del servizio mancante");
        }
        if (req.version() != s.getVersion()) {
            throw ApiException.conflitto("Il servizio è stato modificato da un altro utente. Ricarica e riprova.");
        }
        applica(s, req);
        return ServizioDto.di(servizi.saveAndFlush(s));
    }

    /** Eliminazione logica: il servizio resta nel database (e nelle vendite passate). */
    @Transactional
    public void elimina(Long id) {
        trova(id).elimina(CurrentUser.id());
    }

    private Servizio trova(Long id) {
        return servizi.findById(id).orElseThrow(() -> ApiException.nonTrovato("Servizio non trovato"));
    }

    /** I testi arrivano già puliti (vedi {@link ServizioRequest}). */
    private static void applica(Servizio s, ServizioRequest req) {
        s.setNome(req.nome());
        s.setDescrizione(req.descrizione());
        s.setPrezzi(PrezziArticolo.di(req));
        s.setNote(req.note());
    }
}
