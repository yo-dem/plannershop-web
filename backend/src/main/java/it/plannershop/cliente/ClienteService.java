package it.plannershop.cliente;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.plannershop.cliente.ClienteDtos.ClienteDto;
import it.plannershop.cliente.ClienteDtos.ClienteRequest;
import it.plannershop.common.ApiException;
import it.plannershop.common.Pagina;
import it.plannershop.security.CurrentUser;
import it.plannershop.tenant.TenantRepository;

/** Anagrafica clienti dell'attività corrente. */
@Service
public class ClienteService {

    /** Ordinamenti consentiti: nome usato dal frontend → proprietà dell'entità. */
    private static final Map<String, List<String>> ORDINAMENTI = Map.of(
            "nome", List.of("nome"),
            "cognome", List.of("cognome"),
            "compleanno", List.of("meseNascita", "giornoNascita"),
            "createdAt", List.of("createdAt"),
            "updatedAt", List.of("updatedAt"));

    private final ClienteRepository clienti;
    private final TenantRepository tenants;

    ClienteService(ClienteRepository clienti, TenantRepository tenants) {
        this.clienti = clienti;
        this.tenants = tenants;
    }

    /**
     * Elenco paginato, con ricerca facoltativa su nome e cognome (in qualsiasi
     * ordine), telefono ed email. Senza ordinamento: per cognome e nome.
     */
    @Transactional(readOnly = true)
    public Pagina<ClienteDto> elenco(String q, Pageable richiesta) {
        String testo = q == null || q.isBlank() ? "%" : "%" + proteggiLike(q.trim().toLowerCase()) + "%";
        Pageable pageable = PageRequest.of(richiesta.getPageNumber(), richiesta.getPageSize(),
                ordinamento(richiesta.getSort()));
        return Pagina.di(clienti.cerca(testo, pageable), ClienteDto::di);
    }

    @Transactional(readOnly = true)
    public ClienteDto dettaglio(Long id) {
        return ClienteDto.di(trova(id));
    }

    @Transactional
    public ClienteDto crea(ClienteRequest req) {
        Cliente c = new Cliente(null, null);
        applica(c, req);
        return ClienteDto.di(clienti.saveAndFlush(c));
    }

    @Transactional
    public ClienteDto modifica(Long id, ClienteRequest req) {
        Cliente c = trova(id);
        if (req.version() == null) {
            throw ApiException.richiestaNonValida("Versione del cliente mancante");
        }
        if (req.version() != c.getVersion()) {
            throw ApiException.conflitto("Il cliente è stato modificato da un altro utente. Ricarica e riprova.");
        }
        applica(c, req);
        return ClienteDto.di(clienti.saveAndFlush(c));
    }

    /** Eliminazione logica: il cliente sparisce dagli elenchi ma resta nel database. */
    @Transactional
    public void elimina(Long id) {
        trova(id).elimina(CurrentUser.id());
    }

    /**
     * Clienti che compiono gli anni nel mese indicato, ordinati per giorno.
     * Senza mese: il mese corrente nel fuso orario dell'attività.
     */
    @Transactional(readOnly = true)
    public List<ClienteDto> compleanni(Integer mese) {
        int m = mese != null ? mese : LocalDate.now(fusoOrario()).getMonthValue();
        if (m < 1 || m > 12) {
            throw ApiException.richiestaNonValida("Mese non valido");
        }
        return clienti.findByMeseNascitaOrderByGiornoNascitaAscCognomeAscNomeAsc((short) m).stream()
                .map(ClienteDto::di)
                .toList();
    }

    private Cliente trova(Long id) {
        return clienti.findById(id).orElseThrow(() -> ApiException.nonTrovato("Cliente non trovato"));
    }

    private ZoneId fusoOrario() {
        return CurrentUser.tenantId()
                .flatMap(tenants::findById)
                .map(t -> ZoneId.of(t.getFusoOrario()))
                .orElse(ZoneId.of("Europe/Rome"));
    }

    /** I testi arrivano già puliti (vedi {@link ClienteRequest}). */
    private static void applica(Cliente c, ClienteRequest req) {
        c.setNome(req.nome());
        c.setCognome(req.cognome());
        c.setCompleanno(req.giornoNascita(), req.meseNascita());
        c.setTelefono(req.telefono());
        c.setEmail(req.email() != null ? req.email().toLowerCase() : null);
        c.setIndirizzo(req.indirizzo());
        c.setNote(req.note());
    }

    /** Il testo cercato non deve poter usare i caratteri jolly di LIKE. */
    private static String proteggiLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * Traduce l'ordinamento richiesto ignorando le proprietà non ammesse
     * (eviterebbero un errore 500), poi aggiunge cognome, nome e id per un
     * ordine sempre stabile tra una pagina e l'altra.
     */
    private static Sort ordinamento(Sort richiesto) {
        List<Sort.Order> ordini = new ArrayList<>();
        for (Sort.Order o : richiesto) {
            List<String> proprieta = ORDINAMENTI.get(o.getProperty());
            if (proprieta != null) {
                for (String p : proprieta) {
                    Sort.Order ordine = new Sort.Order(o.getDirection(), p);
                    ordini.add(p.equals("nome") || p.equals("cognome") ? ordine.ignoreCase() : ordine);
                }
            }
        }
        ordini.add(Sort.Order.asc("cognome").ignoreCase());
        ordini.add(Sort.Order.asc("nome").ignoreCase());
        ordini.add(Sort.Order.asc("id"));
        return Sort.by(ordini);
    }
}
