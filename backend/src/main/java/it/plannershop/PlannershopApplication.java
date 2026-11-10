package it.plannershop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PlannershopApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlannershopApplication.class, args);
	}

}
