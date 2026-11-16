package it.plannershop.demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.plannershop.common.ApiException;
import it.plannershop.common.AppProperties;
import it.plannershop.customer.CustomerRepository;
import it.plannershop.customer.CustomerService;
import it.plannershop.product.ProductRepository;
import it.plannershop.product.ProductService;
import it.plannershop.serviceitem.ServiceItemRepository;
import it.plannershop.serviceitem.ServiceItemService;
import it.plannershop.supplier.SupplierRepository;
import it.plannershop.supplier.SupplierService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import tools.jackson.databind.json.JsonMapper;

/**
 * Demo data (customers, suppliers, products, services) for a newly installed
 * business, read from {@code demo/demo-data.json}. They can be loaded only
 * while the business has none of these records, so real data is never mixed
 * with demo data. All the people and companies in the file are fictional.
 */
@Service
public class DemoDataService {

    private static final String RESOURCE = "demo/demo-data.json";

    private final AppProperties app;
    private final JsonMapper json;
    private final Validator validator;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final ServiceItemRepository serviceRepository;
    private final CustomerService customers;
    private final SupplierService suppliers;
    private final ProductService products;
    private final ServiceItemService services;

    DemoDataService(AppProperties app, JsonMapper json, Validator validator,
            CustomerRepository customerRepository, SupplierRepository supplierRepository,
            ProductRepository productRepository, ServiceItemRepository serviceRepository,
            CustomerService customers, SupplierService suppliers, ProductService products,
            ServiceItemService services) {
        this.app = app;
        this.json = json;
        this.validator = validator;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.serviceRepository = serviceRepository;
        this.customers = customers;
        this.suppliers = suppliers;
        this.products = products;
        this.services = services;
    }

    /** Numbers of records created by the import. */
    public record ImportResult(int customers, int suppliers, int products, int services) {
    }

    /** True if the demo data are enabled and the current business is still empty. */
    @Transactional(readOnly = true)
    public boolean available() {
        return app.demoData() && isEmpty();
    }

    /** Creates the demo data in the current business, all or nothing. */
    @Transactional
    public ImportResult load() {
        if (!app.demoData()) {
            throw ApiException.forbidden("demoData.disabled");
        }
        if (!isEmpty()) {
            throw ApiException.conflict("demoData.notEmpty");
        }
        DemoData data = read();
        create(data.customers(), customers::create);
        create(data.suppliers(), suppliers::create);
        create(data.products(), products::create);
        create(data.services(), services::create);
        return new ImportResult(data.customers().size(), data.suppliers().size(),
                data.products().size(), data.services().size());
    }

    private boolean isEmpty() {
        return customerRepository.count() == 0 && supplierRepository.count() == 0
                && productRepository.count() == 0 && serviceRepository.count() == 0;
    }

    private DemoData read() {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            return json.readValue(in, DemoData.class);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + RESOURCE, e);
        }
    }

    /** Validates every request like the controllers do: an invalid file is a bug, not a user error. */
    private <T> void create(List<T> requests, Consumer<T> creator) {
        for (T request : requests) {
            Set<ConstraintViolation<T>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                throw new IllegalStateException("Invalid demo data " + request + ": " + violations);
            }
            creator.accept(request);
        }
    }
}
