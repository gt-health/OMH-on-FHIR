package org.gtri.hdap.mdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gtri.hdap.mdata.common.jpa.entity.ResourceConfig;
import org.gtri.hdap.mdata.common.jpa.repository.ResourceConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sun.nio.ch.IOUtil;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;

import static java.nio.charset.StandardCharsets.UTF_8;

@Configuration
public class InitDatabase {
    private static final Logger logger = LoggerFactory.getLogger(InitDatabase.class);

    @Bean
    CommandLineRunner loadData(ResourceConfigRepository rcRepo) {
        return args -> {
            ObjectMapper mapper = new ObjectMapper();
            ResourceConfig resourceConfig = new ResourceConfig();
            resourceConfig.setResourceId("stepcount_3-1");
            InputStream in = getClass().getResourceAsStream("/init/resourceConfigs/stepcount_3-1.json");
            resourceConfig.setConfig(
                mapper.readTree(IOUtils.toString(in, UTF_8))
            );
            rcRepo.save(resourceConfig);
            logger.debug("Initial resource config saved.");
        };
    }

}
