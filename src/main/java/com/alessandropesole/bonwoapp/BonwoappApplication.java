package com.alessandropesole.bonwoapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BonwoappApplication {

	public static void main(String[] args) {
		SpringApplication.run(BonwoappApplication.class, args);
	}

}
