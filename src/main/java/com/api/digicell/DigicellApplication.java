package com.api.digicell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.api.digicell.config.CorsConfig;

@SpringBootApplication
@Import(CorsConfig.class)
@EnableJpaRepositories("com.api.digicell.repository")
public class DigicellApplication {

	public static void main(String[] args) {
		SpringApplication.run(DigicellApplication.class, args);
	}

}
 