package it.tivusat.cas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class CasIntegrationApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(CasIntegrationApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(CasIntegrationApplication.class);
	}
}