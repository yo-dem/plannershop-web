package it.plannershop.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Una pagina di risultati, in un formato stabile per il frontend
 * (indipendente dalla serializzazione interna di Spring Data).
 *
 * @param elementi   gli elementi della pagina
 * @param totale     numero totale di elementi (tutte le pagine)
 * @param pagina     indice della pagina, da 0
 * @param dimensione elementi per pagina
 */
public record Pagina<T>(List<T> elementi, long totale, int pagina, int dimensione) {

    public static <E, T> Pagina<T> di(Page<E> page, Function<E, T> mapper) {
        return new Pagina<>(page.getContent().stream().map(mapper).toList(), page.getTotalElements(),
                page.getNumber(), page.getSize());
    }
}
