package org.bitmagic.ifeed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@EnableScheduling
public class IFeedApplication {

	public static void main(String[] args) {
		SpringApplication.run(IFeedApplication.class, args);
	}

}
