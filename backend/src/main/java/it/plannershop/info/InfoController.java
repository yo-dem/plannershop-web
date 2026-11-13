package it.plannershop.info;

import java.time.Instant;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.plannershop.common.AppProperties;
import it.plannershop.common.AppProperties.Mode;

/**
 * Diagnostic endpoint: confirms that frontend, backend and database talk to each
 * other, and in which mode (CLOUD or LOCAL) the application runs.
 */
@RestController
@RequestMapping("/api/info")
public class InfoController {

    private final JdbcTemplate jdbc;
    private final ObjectProvider<BuildProperties> build;
    private final AppProperties app;

    public InfoController(JdbcTemplate jdbc, ObjectProvider<BuildProperties> build, AppProperties app) {
        this.jdbc = jdbc;
        this.build = build;
        this.app = app;
    }

    @GetMapping
    public InfoResponse info() {
        BuildProperties props = build.getIfAvailable();
        String version = props != null ? props.getVersion() : "dev";
        String database = jdbc.queryForObject("SHOW server_version", String.class);
        return new InfoResponse("PlannerShop", version, "PostgreSQL " + database, app.mode(), Instant.now());
    }

    public record InfoResponse(String application, String version, String database, Mode mode, Instant serverTime) {
    }
}
