package org.gtri.hdap.mdata.conf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.gtri.hdap.mdata.jackson.HapiStu3Serializer;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by es130 on 7/20/2018.
 */
@Configuration
public class JacksonConfig {

    Logger logger = LoggerFactory.getLogger(JacksonConfig.class);

    @Bean
    public ObjectMapper objectMapper(){
        logger.debug("Configuring Object Mapper");
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule hapiDstu3Module = new SimpleModule("HapiDSTU3Module");
        hapiDstu3Module.addSerializer(new HapiStu3Serializer(IBaseResource.class));
        objectMapper.registerModule(hapiDstu3Module);
        return objectMapper;
    }
}
