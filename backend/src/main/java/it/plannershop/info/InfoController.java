package it.plannershop.info;

import java.time.Instant;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint di diagnostica: conferma che frontend, backend e database
 * comunicano. Verrà protetto (o ridotto) quando arriverà la sicurezza.
 */
@RestController
@RequestMapping("/api/info")
public class InfoController {

    private final JdbcTemplate jdbc;
    private final ObjectProvider<BuildProperties> build;

    public InfoController(JdbcTemplate jdbc, ObjectProvider<BuildProperties> build) {
        this.jdbc = jdbc;
        this.build = build;
    }

    @GetMapping
    public InfoResponse info() {
        BuildProperties props = build.getIfAvailable();
        String version = props != null ? props.getVersion() : "dev";
        String database = jdbc.queryForObject("SHOW server_version", String.class);
        return new InfoResponse("PlannerShop", version, "PostgreSQL " + database, Instant.now());
    }

    public record InfoResponse(String applicazione, String versione, String database, Instant oraServer) {
    }
}
