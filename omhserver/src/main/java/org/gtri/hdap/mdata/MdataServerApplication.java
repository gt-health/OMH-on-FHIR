package org.gtri.hdap.mdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "org.gtri.hdap.mdata.jpa.repository")
public class MdataServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MdataServerApplication.class, args);
	}
}
