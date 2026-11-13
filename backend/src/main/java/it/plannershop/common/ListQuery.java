package it.plannershop.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Helpers shared by lists with search, sorting and pagination. */
public final class ListQuery {

    private ListQuery() {
    }

    /**
     * Text to search with LIKE: lower case, wrapped in %, with wildcards
     * escaped (use with {@code escape '\'}). No text: everything.
     */
    public static String likePattern(String q) {
        if (q == null || q.isBlank()) {
            return "%";
        }
        String s = q.strip().toLowerCase();
        return "%" + s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    /**
     * Translates the sort requested by the frontend, ignoring properties that
     * are not allowed (they would cause a 500 error), then appends the default
     * order, which keeps the list stable from one page to the next.
     *
     * @param allowed      name used by the frontend → entity properties
     * @param textual      text properties, sorted case-insensitively
     * @param defaultOrder always appended (must end with a unique field, e.g. id)
     */
    public static Pageable pageable(Pageable request, Map<String, List<String>> allowed, Set<String> textual,
            List<Sort.Order> defaultOrder) {
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order o : request.getSort()) {
            for (String p : allowed.getOrDefault(o.getProperty(), List.of())) {
                Sort.Order order = new Sort.Order(o.getDirection(), p);
                orders.add(textual.contains(p) ? order.ignoreCase() : order);
            }
        }
        orders.addAll(defaultOrder);
        return PageRequest.of(request.getPageNumber(), request.getPageSize(), Sort.by(orders));
    }
}
