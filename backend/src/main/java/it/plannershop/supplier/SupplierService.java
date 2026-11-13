package it.plannershop.supplier;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.plannershop.common.ApiException;
import it.plannershop.common.ListQuery;
import it.plannershop.common.PageResponse;
import it.plannershop.security.CurrentUser;
import it.plannershop.supplier.SupplierDtos.SupplierDto;
import it.plannershop.supplier.SupplierDtos.SupplierRequest;

/** Suppliers of the current tenant. */
@Service
public class SupplierService {

    /** Allowed sort keys: name used by the frontend → entity properties. */
    private static final Map<String, List<String>> SORTS = Map.of(
            "name", List.of("name"),
            "createdAt", List.of("createdAt"),
            "updatedAt", List.of("updatedAt"));

    private static final List<Sort.Order> DEFAULT_ORDER = List.of(Sort.Order.asc("name").ignoreCase(),
            Sort.Order.asc("id"));

    private final SupplierRepository suppliers;

    SupplierService(SupplierRepository suppliers) {
        this.suppliers = suppliers;
    }

    /** Paged list with optional search on all text fields. Without a sort: by name. */
    @Transactional(readOnly = true)
    public PageResponse<SupplierDto> list(String q, Pageable request) {
        Pageable pageable = ListQuery.pageable(request, SORTS, Set.of("name"), DEFAULT_ORDER);
        String ibanPattern = ListQuery.likePattern(q == null ? null : q.replaceAll("\\s", ""));
        return PageResponse.of(suppliers.search(ListQuery.likePattern(q), ibanPattern, pageable), SupplierDto::of);
    }

    @Transactional(readOnly = true)
    public SupplierDto get(Long id) {
        return SupplierDto.of(find(id));
    }

    @Transactional
    public SupplierDto create(SupplierRequest req) {
        Supplier s = new Supplier(null);
        apply(s, req);
        return SupplierDto.of(suppliers.saveAndFlush(s));
    }

    @Transactional
    public SupplierDto update(Long id, SupplierRequest req) {
        Supplier s = find(id);
        if (req.version() == null) {
            throw ApiException.badRequest("error.versionMissing");
        }
        if (req.version() != s.getVersion()) {
            throw ApiException.conflict("error.conflict");
        }
        apply(s, req);
        return SupplierDto.of(suppliers.saveAndFlush(s));
    }

    /** Soft delete: the supplier stays in the database. */
    @Transactional
    public void delete(Long id) {
        find(id).delete(CurrentUser.id());
    }

    private Supplier find(Long id) {
        return suppliers.findById(id).orElseThrow(() -> ApiException.notFound("error.notFound.supplier"));
    }

    /** Text is already cleaned and the IBAN normalized (see {@link SupplierRequest}). */
    private static void apply(Supplier s, SupplierRequest req) {
        s.setName(req.name());
        s.setAddress(req.address());
        s.setPhone(req.phone());
        s.setEmail(req.email() != null ? req.email().toLowerCase() : null);
        s.setIban(req.iban());
        s.setNotes(req.notes());
    }
}
