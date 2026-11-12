package it.plannershop.cliente;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Le query vedono solo i clienti dell'attività corrente (@TenantId) e non
 * eliminati (@SQLRestriction): nessun filtro va aggiunto a mano.
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Ricerca per nome e cognome (in entrambi gli ordini), telefono o email.
     *
     * @param testo già in minuscolo, con % e _ protetti, racchiuso tra %
     */
    @Query("""
            select c from Cliente c
            where lower(concat(c.nome, ' ', c.cognome)) like :testo escape '\\'
               or lower(concat(c.cognome, ' ', c.nome)) like :testo escape '\\'
               or lower(c.telefono) like :testo escape '\\'
               or lower(c.email) like :testo escape '\\'
            """)
    Page<Cliente> cerca(String testo, Pageable pageable);

    List<Cliente> findByMeseNascitaOrderByGiornoNascitaAscCognomeAscNomeAsc(Short mese);
}
