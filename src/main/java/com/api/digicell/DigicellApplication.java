package com.api.digicell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.api.digicell.config.CorsConfig;

@SpringBootApplication
@Import(CorsConfig.class)
public class DigicellApplication {

	public static void main(String[] args) {
		SpringApplication.run(DigicellApplication.class, args);
	}

}
 