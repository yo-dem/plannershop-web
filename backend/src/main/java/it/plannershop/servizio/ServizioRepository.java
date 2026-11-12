package it.plannershop.servizio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Solo servizi dell'attività corrente (@TenantId) e non eliminati (@SQLRestriction). */
public interface ServizioRepository extends JpaRepository<Servizio, Long> {

    /**
     * Ricerca su nome, descrizione e note.
     *
     * @param testo vedi {@link it.plannershop.common.Ricerca#testoLike(String)}
     */
    @Query("""
            select s from Servizio s
            where lower(s.nome) like :testo escape '\\'
               or lower(s.descrizione) like :testo escape '\\'
               or lower(s.note) like :testo escape '\\'
            """)
    Page<Servizio> cerca(String testo, Pageable pageable);
}
