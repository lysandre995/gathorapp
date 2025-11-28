package com.alfano.gathorapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application class for GathorApp.
 * Enables scheduling for automated tasks (chat/voucher expiration).
 */
@SpringBootApplication
@EnableScheduling
public class GathorappApplication {

	public static void main(String[] args) {
		SpringApplication.run(GathorappApplication.class, args);
	}

}
