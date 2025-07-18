package com.medco.HealthConnectProvider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = "com.medco.HealthConnectProvider")
@EnableScheduling
public class HealthConnectProviderApplication {

	public static void main(String[] args) {
		SpringApplication.run(HealthConnectProviderApplication.class, args);
	}

}
