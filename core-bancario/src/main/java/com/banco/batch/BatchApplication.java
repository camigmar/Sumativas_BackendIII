package com.banco.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.banco.batch", "com.banco.bff", "com.banco.security"})
@EntityScan(basePackages = {"com.banco.batch.model", "com.banco.security.model"})
@EnableJpaRepositories(basePackages = {"com.banco.batch.repository", "com.banco.security.repository"})
@EnableBatchProcessing
public class BatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatchApplication.class, args);
	}

}
