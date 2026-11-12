package it.plannershop.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Utilità comuni agli elenchi con ricerca, ordinamento e paginazione. */
public final class Ricerca {

    private Ricerca() {
    }

    /**
     * Testo da cercare con LIKE: minuscolo, racchiuso tra %, con i caratteri
     * jolly protetti (si usa con {@code escape '\'}). Senza testo: tutto.
     */
    public static String testoLike(String q) {
        if (q == null || q.isBlank()) {
            return "%";
        }
        String s = q.strip().toLowerCase();
        return "%" + s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    /**
     * Traduce l'ordinamento richiesto dal frontend ignorando le proprietà non
     * ammesse (darebbero un errore 500), poi aggiunge l'ordine predefinito, che
     * rende l'elenco stabile tra una pagina e l'altra.
     *
     * @param ammessi       nome usato dal frontend → proprietà dell'entità
     * @param testuali      proprietà di testo, ordinate senza badare alle maiuscole
     * @param predefinito   ordine sempre aggiunto in coda (deve finire con un campo univoco, es. id)
     */
    public static Pageable pagina(Pageable richiesta, Map<String, List<String>> ammessi, Set<String> testuali,
            List<Sort.Order> predefinito) {
        List<Sort.Order> ordini = new ArrayList<>();
        for (Sort.Order o : richiesta.getSort()) {
            for (String p : ammessi.getOrDefault(o.getProperty(), List.of())) {
                Sort.Order ordine = new Sort.Order(o.getDirection(), p);
                ordini.add(testuali.contains(p) ? ordine.ignoreCase() : ordine);
            }
        }
        ordini.addAll(predefinito);
        return PageRequest.of(richiesta.getPageNumber(), richiesta.getPageSize(), Sort.by(ordini));
    }
}
