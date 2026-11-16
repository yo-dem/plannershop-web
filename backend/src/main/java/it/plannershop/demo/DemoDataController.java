package it.plannershop.demo;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import it.plannershop.demo.DemoDataService.ImportResult;
import it.plannershop.demo.DemoDataService.Status;

/** Demo data for an empty business, and reset to the demo data: ADMIN only. */
@RestController
@RequestMapping("/api/demo-data")
@PreAuthorize("hasRole('ADMIN')")
public class DemoDataController {

    private final DemoDataService service;

    DemoDataController(DemoDataService service) {
        this.service = service;
    }

    /** @param confirmName name of the business, typed by the user to confirm */
    public record ResetRequest(String confirmName) {
    }

    @GetMapping
    Status status() {
        return service.status();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ImportResult load() {
        return service.load();
    }

    @PostMapping("/reset")
    ImportResult reset(@RequestBody ResetRequest body) {
        return service.reset(body.confirmName());
    }
}
