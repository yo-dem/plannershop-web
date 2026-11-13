package it.plannershop.serviceitem;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.plannershop.common.ApiException;
import it.plannershop.common.ItemPrices;
import it.plannershop.common.ListQuery;
import it.plannershop.common.PageResponse;
import it.plannershop.security.CurrentUser;
import it.plannershop.serviceitem.ServiceItemDtos.ServiceItemDto;
import it.plannershop.serviceitem.ServiceItemDtos.ServiceItemRequest;

/** Services offered by the current tenant. */
@Service
public class ServiceItemService {

    /** Allowed sort keys: name used by the frontend → entity properties. */
    private static final Map<String, List<String>> SORTS = Map.of(
            "name", List.of("name"),
            "description", List.of("description"),
            "salePrice", List.of("prices.salePrice"),
            "createdAt", List.of("createdAt"),
            "updatedAt", List.of("updatedAt"));

    private static final List<Sort.Order> DEFAULT_ORDER = List.of(
            Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("description").ignoreCase(), Sort.Order.asc("id"));

    private final ServiceItemRepository services;

    ServiceItemService(ServiceItemRepository services) {
        this.services = services;
    }

    /** Paged list with optional search on name, description and notes. Without a sort: by name. */
    @Transactional(readOnly = true)
    public PageResponse<ServiceItemDto> list(String q, Pageable request) {
        Pageable pageable = ListQuery.pageable(request, SORTS, Set.of("name", "description"), DEFAULT_ORDER);
        return PageResponse.of(services.search(ListQuery.likePattern(q), pageable), ServiceItemDto::of);
    }

    @Transactional(readOnly = true)
    public ServiceItemDto get(Long id) {
        return ServiceItemDto.of(find(id));
    }

    @Transactional
    public ServiceItemDto create(ServiceItemRequest req) {
        ServiceItem s = new ServiceItem(null, null);
        apply(s, req);
        return ServiceItemDto.of(services.saveAndFlush(s));
    }

    @Transactional
    public ServiceItemDto update(Long id, ServiceItemRequest req) {
        ServiceItem s = find(id);
        if (req.version() == null) {
            throw ApiException.badRequest("error.versionMissing");
        }
        if (req.version() != s.getVersion()) {
            throw ApiException.conflict("error.conflict");
        }
        apply(s, req);
        return ServiceItemDto.of(services.saveAndFlush(s));
    }

    /** Soft delete: the service stays in the database (and in past sales). */
    @Transactional
    public void delete(Long id) {
        find(id).delete(CurrentUser.id());
    }

    private ServiceItem find(Long id) {
        return services.findById(id).orElseThrow(() -> ApiException.notFound("error.notFound.service"));
    }

    /** Text is already cleaned (see {@link ServiceItemRequest}). */
    private static void apply(ServiceItem s, ServiceItemRequest req) {
        s.setName(req.name());
        s.setDescription(req.description());
        s.setPrices(ItemPrices.from(req));
        s.setNotes(req.notes());
    }
}
