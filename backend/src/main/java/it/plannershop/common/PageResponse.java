package it.plannershop.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * A page of results, in a stable format for the frontend (independent of
 * Spring Data's internal serialization).
 *
 * @param items the items of this page
 * @param total total number of items (all pages)
 * @param page  page index, from 0
 * @param size  items per page
 */
public record PageResponse<T>(List<T> items, long total, int page, int size) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getTotalElements(),
                page.getNumber(), page.getSize());
    }
}
