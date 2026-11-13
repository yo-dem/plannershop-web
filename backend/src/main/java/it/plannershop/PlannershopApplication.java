package it.plannershop;

import java.util.Arrays;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PlannershopApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(PlannershopApplication.class);
		// Password reset (--app.reset-password=<email>): the web server uses any free
		// port, so it can run even while the application is already running on 8080.
		// The process exits right after the reset.
		if (Arrays.stream(args).anyMatch(a -> a.startsWith("--app.reset-password"))) {
			app.setDefaultProperties(Map.of("server.port", "0"));
		}
		app.run(args);
	}

}
