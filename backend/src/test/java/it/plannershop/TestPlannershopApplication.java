package it.plannershop;

import org.springframework.boot.SpringApplication;

public class TestPlannershopApplication {

	public static void main(String[] args) {
		SpringApplication.from(PlannershopApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
