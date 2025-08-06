package com.example.companybackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CompanyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CompanyBackendApplication.class, args);
	}

}
