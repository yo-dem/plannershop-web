package it.plannershop.cliente;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.plannershop.cliente.ClienteDtos.ClienteDto;
import it.plannershop.cliente.ClienteDtos.ClienteRequest;
import it.plannershop.common.ApiException;
import it.plannershop.common.Pagina;
import it.plannershop.common.Ricerca;
import it.plannershop.security.CurrentUser;
import it.plannershop.tenant.OrologioAttivita;

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

    /** In coda a ogni ordinamento: cognome, nome, id (ordine stabile tra le pagine). */
    private static final List<Sort.Order> PREDEFINITO = List.of(
            Sort.Order.asc("cognome").ignoreCase(), Sort.Order.asc("nome").ignoreCase(), Sort.Order.asc("id"));

    private final ClienteRepository clienti;
    private final OrologioAttivita orologio;

    ClienteService(ClienteRepository clienti, OrologioAttivita orologio) {
        this.clienti = clienti;
        this.orologio = orologio;
    }

    /**
     * Elenco paginato, con ricerca facoltativa su nome e cognome (in qualsiasi
     * ordine), telefono ed email. Senza ordinamento: per cognome e nome.
     */
    @Transactional(readOnly = true)
    public Pagina<ClienteDto> elenco(String q, Pageable richiesta) {
        Pageable pageable = Ricerca.pagina(richiesta, ORDINAMENTI, Set.of("nome", "cognome"), PREDEFINITO);
        return Pagina.di(clienti.cerca(Ricerca.testoLike(q), pageable), ClienteDto::di);
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
        int m = mese != null ? mese : orologio.oggi().getMonthValue();
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
}
