package com.example.companybackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableTransactionManagement
public class CompanyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CompanyBackendApplication.class, args);
	}

}
