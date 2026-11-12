package it.plannershop.prodotto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Solo prodotti dell'attività corrente (@TenantId) e non eliminati (@SQLRestriction). */
public interface ProdottoRepository extends JpaRepository<Prodotto, Long> {

    /**
     * Ricerca su marca e descrizione (in entrambi gli ordini) e note.
     *
     * @param testo       vedi {@link it.plannershop.common.Ricerca#testoLike(String)}
     * @param disponibili true = solo prodotti con quantità maggiore di zero
     */
    @Query("""
            select p from Prodotto p
            where (lower(concat(p.marca, ' ', p.descrizione)) like :testo escape '\\'
                   or lower(concat(p.descrizione, ' ', p.marca)) like :testo escape '\\'
                   or lower(p.note) like :testo escape '\\')
              and (:disponibili = false or p.quantita > 0)
            """)
    Page<Prodotto> cerca(String testo, boolean disponibili, Pageable pageable);
}
