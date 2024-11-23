package com.turtrack.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TurtrackServerApplication {
//	static {
//		System.setProperty("spring.profiles.active", "prod");
//	}

	public static void main(String[] args) {
		SpringApplication.run(TurtrackServerApplication.class, args);
	}

}
