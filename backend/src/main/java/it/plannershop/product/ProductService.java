package it.plannershop.product;

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
import it.plannershop.product.ProductDtos.ProductDto;
import it.plannershop.product.ProductDtos.ProductRequest;
import it.plannershop.security.CurrentUser;
import it.plannershop.tenant.TenantClock;

/** Products and stock of the current tenant. */
@Service
public class ProductService {

    /** Allowed sort keys: name used by the frontend → entity properties. */
    private static final Map<String, List<String>> SORTS = Map.of(
            "brand", List.of("brand", "description"),
            "description", List.of("description"),
            "quantity", List.of("quantity"),
            "salePrice", List.of("prices.salePrice"),
            "stockDate", List.of("stockDate"),
            "createdAt", List.of("createdAt"),
            "updatedAt", List.of("updatedAt"));

    private static final List<Sort.Order> DEFAULT_ORDER = List.of(
            Sort.Order.asc("brand").ignoreCase(), Sort.Order.asc("description").ignoreCase(), Sort.Order.asc("id"));

    private final ProductRepository products;
    private final TenantClock clock;

    ProductService(ProductRepository products, TenantClock clock) {
        this.products = products;
        this.clock = clock;
    }

    /**
     * Paged list with optional search on brand, description and notes.
     * Without a sort: by brand and description.
     *
     * @param availableOnly only products with quantity greater than zero (for sales)
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductDto> list(String q, boolean availableOnly, Pageable request) {
        Pageable pageable = ListQuery.pageable(request, SORTS, Set.of("brand", "description"), DEFAULT_ORDER);
        return PageResponse.of(products.search(ListQuery.likePattern(q), availableOnly, pageable), ProductDto::of);
    }

    @Transactional(readOnly = true)
    public ProductDto get(Long id) {
        return ProductDto.of(find(id));
    }

    @Transactional
    public ProductDto create(ProductRequest req) {
        Product p = new Product(null, null);
        apply(p, req);
        if (p.getStockDate() == null) {
            p.setStockDate(clock.today());
        }
        return ProductDto.of(products.saveAndFlush(p));
    }

    @Transactional
    public ProductDto update(Long id, ProductRequest req) {
        Product p = find(id);
        if (req.version() == null) {
            throw ApiException.badRequest("error.versionMissing");
        }
        if (req.version() != p.getVersion()) {
            throw ApiException.conflict("error.conflict");
        }
        apply(p, req);
        return ProductDto.of(products.saveAndFlush(p));
    }

    /** Soft delete: the product stays in the database (and in past sales). */
    @Transactional
    public void delete(Long id) {
        find(id).delete(CurrentUser.id());
    }

    private Product find(Long id) {
        return products.findById(id).orElseThrow(() -> ApiException.notFound("error.notFound.product"));
    }

    /** Text is already cleaned (see {@link ProductRequest}). */
    private static void apply(Product p, ProductRequest req) {
        p.setStockDate(req.stockDate());
        p.setBrand(req.brand());
        p.setDescription(req.description());
        p.setQuantity(req.quantity());
        p.setPrices(ItemPrices.from(req));
        p.setNotes(req.notes());
    }
}
