package org.gtri.hdap.mdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gtri.hdap.mdata.common.jpa.entity.FhirTemplate;
import org.gtri.hdap.mdata.common.jpa.entity.ResourceConfig;
import org.gtri.hdap.mdata.common.jpa.repository.FhirTemplateRepository;
import org.gtri.hdap.mdata.common.jpa.repository.ResourceConfigRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import sun.nio.ch.IOUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

@Configuration
public class InitDatabase {

    /*========================================================================*/
	/* CONSTANTS */
    /*========================================================================*/
    public static String CATALOG_JSON_MAPPING_ARRAY_NAME = "mappings";
    public static String CATALOG_JSON_MAPPING_TEMPLATE_ELEMENT_NAME = "template";
    public static String CATALOG_JSON_MAPPING_CONFIG_ELEMENT_NAME = "config";

    /*========================================================================*/
	/* VARIABLES */
    /*========================================================================*/
    private static final Logger logger = LoggerFactory.getLogger(InitDatabase.class);

    @Bean
    CommandLineRunner loadData(ResourceConfigRepository rcRepo, FhirTemplateRepository ftRepo) {
        return args -> {

            logger.debug("Loading mapping config files");
            //load json templates and configs
            InputStream catalogInputStream = getClassPathResourceAsInputStream("catalog.json");;
            parseCatalogFile(catalogInputStream, rcRepo, ftRepo);
            logger.debug("Finished loading mapping config files");
        };
    }

    private InputStream getClassPathResourceAsInputStream(String resourcePath){
        Resource configCatalogResource = new ClassPathResource(resourcePath);
        InputStream catalogInputStream = null;
        try {
            catalogInputStream = configCatalogResource.getInputStream();
        }
        catch(IOException ioe){
            logger.warn("Could not get input stream for catalog.json");
        }
        return catalogInputStream;
    }

    private void parseCatalogFile(InputStream catalogInputStream, ResourceConfigRepository rcRepo, FhirTemplateRepository ftRepo) throws IOException{
        logger.debug("Parsing Catalog File");
        if( catalogInputStream != null ) {
            JSONTokener tokener = new JSONTokener(catalogInputStream);
            JSONObject catalogJsonObj = new JSONObject(tokener);
            JSONArray catalogMappings = catalogJsonObj.getJSONArray(CATALOG_JSON_MAPPING_ARRAY_NAME);
            logger.debug("Parsing mappings");
            int numMappings = catalogMappings.length();
            JSONObject currMappingObject = null;
            for (int i = 0; i < numMappings; i++) {
                currMappingObject = catalogMappings.getJSONObject(i);
                parseMappingConfig(currMappingObject, rcRepo, ftRepo);
            }
        }
        else{
            logger.warn("Could not load OMH to FHIR configuration files.");
        }
        logger.debug("Finished Parsing Catalog File");
    }

    private void parseMappingConfig(JSONObject currMappingObject, ResourceConfigRepository rcRepo, FhirTemplateRepository ftRepo) throws IOException{
        logger.debug("Parsing Mapping template");
        String currTemplate = currMappingObject.getString(CATALOG_JSON_MAPPING_TEMPLATE_ELEMENT_NAME);
        String currConfig = currMappingObject.getString(CATALOG_JSON_MAPPING_CONFIG_ELEMENT_NAME);
        logger.debug("Curr Config " + currConfig);

        //templates will be of the format resourceConfigs/dstu3_step-count.json
        //parse teh config to get the
        Pattern p = Pattern.compile("\\w+/(\\w+_[\\w-]+).json");
        Matcher m = p.matcher(currConfig);
        m.find();
        String configName = m.group(1);
        logger.debug("Name of config: " + configName);
        ObjectMapper mapper = new ObjectMapper();
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.setResourceId(configName);
        InputStream in = getClassPathResourceAsInputStream(currConfig);
        resourceConfig.setConfig(mapper.readTree(IOUtils.toString(in, UTF_8)));
        rcRepo.save(resourceConfig);

        logger.debug("Curr Template " + currTemplate);
        m = p.matcher(currTemplate);
        m.find();
        configName = m.group(1);
        logger.debug("Name of template: " + configName);
        FhirTemplate fhirTemplate = new FhirTemplate();
        fhirTemplate.setTemplateId(configName);
        in = getClassPathResourceAsInputStream(currTemplate);
        fhirTemplate.setTemplate(mapper.readTree(IOUtils.toString(in, UTF_8)));
        ftRepo.save(fhirTemplate);
        logger.debug("Finished Parsing Mapping template");
    }

}
