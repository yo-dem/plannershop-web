package it.plannershop.fornitore;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.plannershop.common.ApiException;
import it.plannershop.common.Pagina;
import it.plannershop.common.Ricerca;
import it.plannershop.fornitore.FornitoreDtos.FornitoreDto;
import it.plannershop.fornitore.FornitoreDtos.FornitoreRequest;
import it.plannershop.security.CurrentUser;

/** Fornitori dell'attività corrente. */
@Service
public class FornitoreService {

    /** Ordinamenti consentiti: nome usato dal frontend → proprietà dell'entità. */
    private static final Map<String, List<String>> ORDINAMENTI = Map.of(
            "nome", List.of("nome"),
            "createdAt", List.of("createdAt"),
            "updatedAt", List.of("updatedAt"));

    private static final List<Sort.Order> PREDEFINITO = List.of(Sort.Order.asc("nome").ignoreCase(),
            Sort.Order.asc("id"));

    private final FornitoreRepository fornitori;

    FornitoreService(FornitoreRepository fornitori) {
        this.fornitori = fornitori;
    }

    /** Elenco paginato con ricerca facoltativa su tutti i campi di testo. Senza ordinamento: per nome. */
    @Transactional(readOnly = true)
    public Pagina<FornitoreDto> elenco(String q, Pageable richiesta) {
        Pageable pageable = Ricerca.pagina(richiesta, ORDINAMENTI, Set.of("nome"), PREDEFINITO);
        String testoIban = Ricerca.testoLike(q == null ? null : q.replaceAll("\\s", ""));
        return Pagina.di(fornitori.cerca(Ricerca.testoLike(q), testoIban, pageable), FornitoreDto::di);
    }

    @Transactional(readOnly = true)
    public FornitoreDto dettaglio(Long id) {
        return FornitoreDto.di(trova(id));
    }

    @Transactional
    public FornitoreDto crea(FornitoreRequest req) {
        Fornitore f = new Fornitore(null);
        applica(f, req);
        return FornitoreDto.di(fornitori.saveAndFlush(f));
    }

    @Transactional
    public FornitoreDto modifica(Long id, FornitoreRequest req) {
        Fornitore f = trova(id);
        if (req.version() == null) {
            throw ApiException.richiestaNonValida("Versione del fornitore mancante");
        }
        if (req.version() != f.getVersion()) {
            throw ApiException.conflitto("Il fornitore è stato modificato da un altro utente. Ricarica e riprova.");
        }
        applica(f, req);
        return FornitoreDto.di(fornitori.saveAndFlush(f));
    }

    /** Eliminazione logica: il fornitore resta nel database. */
    @Transactional
    public void elimina(Long id) {
        trova(id).elimina(CurrentUser.id());
    }

    private Fornitore trova(Long id) {
        return fornitori.findById(id).orElseThrow(() -> ApiException.nonTrovato("Fornitore non trovato"));
    }

    /** I testi arrivano già puliti e l'IBAN normalizzato (vedi {@link FornitoreRequest}). */
    private static void applica(Fornitore f, FornitoreRequest req) {
        f.setNome(req.nome());
        f.setIndirizzo(req.indirizzo());
        f.setTelefono(req.telefono());
        f.setEmail(req.email() != null ? req.email().toLowerCase() : null);
        f.setIban(req.iban());
        f.setNote(req.note());
    }
}
