package org.bitmagic.ifeed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
@EnableAsync
public class IFeedApplication {

	@Configuration
	public static class CorsMvcConfigurer implements WebMvcConfigurer {
		@Override
		public void addCorsMappings(CorsRegistry registry) {

			registry.addMapping("/api/**")
					.allowedOrigins("https://www.ifeed.cc","http://localhost:5173")
					.allowedMethods("PUT", "DELETE", "POST", "GET", "PATCH", "OPTIONS")
					.allowedHeaders("*")
					.allowCredentials(true).maxAge(3600);

			// Add more mappings...
		}

	}


	public static void main(String[] args) {
		SpringApplication.run(IFeedApplication.class, args);
	}

}
