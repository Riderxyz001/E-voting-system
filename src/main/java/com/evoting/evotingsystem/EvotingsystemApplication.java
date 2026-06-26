package com.evoting.evotingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EvotingsystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(EvotingsystemApplication.class, args);
	}

	@jakarta.annotation.PostConstruct
	public void init() {
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
	}
}
