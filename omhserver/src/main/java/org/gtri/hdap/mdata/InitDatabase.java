package org.gtri.hdap.mdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gtri.hdap.mdata.common.jpa.entity.FhirTemplate;
import org.gtri.hdap.mdata.common.jpa.entity.ResourceConfig;
import org.gtri.hdap.mdata.common.jpa.repository.FhirTemplateRepository;
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
    CommandLineRunner loadData(ResourceConfigRepository rcRepo, FhirTemplateRepository ftRepo) {
        return args -> {
            ObjectMapper mapper = new ObjectMapper();
            ResourceConfig resourceConfig = new ResourceConfig();
            resourceConfig.setResourceId("dstu3_step_count");
            InputStream in = getClass().getResourceAsStream("/init/resourceConfigs/dstu3_step_count.json");
            resourceConfig.setConfig(mapper.readTree(IOUtils.toString(in, UTF_8)));
            rcRepo.save(resourceConfig);
            FhirTemplate fhirTemplate = new FhirTemplate();
            fhirTemplate.setTemplateId("dstu3_Observation");
            in = getClass().getResourceAsStream("/init/fhirTemplates/dstu3_Observation.json");
            fhirTemplate.setTemplate(mapper.readTree(IOUtils.toString(in, UTF_8)));
            ftRepo.save(fhirTemplate);
            logger.debug("Initial resource config saved.");
        };
    }

}
