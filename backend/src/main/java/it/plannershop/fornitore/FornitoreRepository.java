package it.plannershop.fornitore;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Solo fornitori dell'attività corrente (@TenantId) e non eliminati (@SQLRestriction). */
public interface FornitoreRepository extends JpaRepository<Fornitore, Long> {

    /**
     * Ricerca su nome, indirizzo, telefono, email, IBAN e note.
     *
     * @param testo     vedi {@link it.plannershop.common.Ricerca#testoLike(String)}
     * @param testoIban come testo, ma senza spazi (l'IBAN è salvato senza spazi)
     */
    @Query("""
            select f from Fornitore f
            where lower(f.nome) like :testo escape '\\'
               or lower(f.indirizzo) like :testo escape '\\'
               or lower(f.telefono) like :testo escape '\\'
               or lower(f.email) like :testo escape '\\'
               or lower(f.iban) like :testoIban escape '\\'
               or lower(f.note) like :testo escape '\\'
            """)
    Page<Fornitore> cerca(String testo, String testoIban, Pageable pageable);
}
