package it.plannershop.demo;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import it.plannershop.demo.DemoDataService.ImportResult;

/** Demo data for an empty business: only the ADMIN can load them. */
@RestController
@RequestMapping("/api/demo-data")
@PreAuthorize("hasRole('ADMIN')")
public class DemoDataController {

    private final DemoDataService service;

    DemoDataController(DemoDataService service) {
        this.service = service;
    }

    public record DemoDataStatus(boolean available) {
    }

    @GetMapping
    DemoDataStatus status() {
        return new DemoDataStatus(service.available());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ImportResult load() {
        return service.load();
    }
}
