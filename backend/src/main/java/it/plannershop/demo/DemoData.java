package it.plannershop.demo;

import java.util.List;

import it.plannershop.customer.CustomerDtos.CustomerRequest;
import it.plannershop.product.ProductDtos.ProductRequest;
import it.plannershop.serviceitem.ServiceItemDtos.ServiceItemRequest;
import it.plannershop.supplier.SupplierDtos.SupplierRequest;

/**
 * Content of {@code demo/demo-data.json}: the same requests the frontend sends
 * when it creates the records, so they go through the same validation and
 * price calculation.
 */
record DemoData(
        List<CustomerRequest> customers,
        List<SupplierRequest> suppliers,
        List<ProductRequest> products,
        List<ServiceItemRequest> services) {
}
