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
		// Ripristino password (--app.reset-password=<email>): il server web usa una
		// porta libera qualsiasi, così si può eseguire anche mentre l'applicazione
		// è già avviata sulla 8080. Il processo termina subito dopo il ripristino.
		if (Arrays.stream(args).anyMatch(a -> a.startsWith("--app.reset-password"))) {
			app.setDefaultProperties(Map.of("server.port", "0"));
		}
		app.run(args);
	}

}
