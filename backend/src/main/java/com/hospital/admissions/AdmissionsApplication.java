package com.hospital.admissions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Intelligent Patient Flow and Bed Capacity Orchestration System.
 */
@EnableScheduling
@SpringBootApplication
public class AdmissionsApplication {

    /**
     * Bootstraps and starts the Spring Boot microservice.
     *
     * @param args command-line launch arguments.
     */
	public static void main(String[] args) {
		SpringApplication.run(AdmissionsApplication.class, args);
	}

}
