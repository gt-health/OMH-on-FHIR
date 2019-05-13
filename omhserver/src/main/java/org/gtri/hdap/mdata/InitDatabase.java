package org.gtri.hdap.mdata;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
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
    public static String JSON_FILE_PATTERN = "\\w+/(\\w+_[\\w-]+).json";

    /*========================================================================*/
	/* VARIABLES */
    /*========================================================================*/
    private static final Logger logger = LoggerFactory.getLogger(InitDatabase.class);
    private Pattern jsonFilePattern = Pattern.compile(JSON_FILE_PATTERN);

    /**
     * Runs at application start up to load the config files into the database
     * @param rcRepo the ResourceConfigRepository to use
     * @param ftRepo the FhirTemplateRepository to use
     * @return the function to call at start up
     */
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

    /**
     * Returns the InputStream for the resource located at resourcePath
     * @param resourcePath the path to the classpath resource to locate
     * @return an InputStream for the resource if it is found, or null otherwise
     */
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

    /**
     * Parses the catalog.json file that describes how config and template resources files are used.
     * @param catalogInputStream the InputStream to the catalog
     * @param rcRepo the ResourceConfigRepository to use
     * @param ftRepo the FhirTemplateRepository to use
     * @throws IOException thrown if the resource or template file on the classpath cannot be retrieved as an InputStream
     */
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

    /**
     * Parses a mapping entry in the catalog.json file.
     * @param currMappingObject the mapping object to parse
     * @param rcRepo the ResourceConfigRepository to use
     * @param ftRepo the FhirTemplateRepository to use
     * @throws IOException thrown if the resource or template file on the classpath cannot be retrieved as an InputStream
     */
    private void parseMappingConfig(JSONObject currMappingObject, ResourceConfigRepository rcRepo, FhirTemplateRepository ftRepo) throws IOException{
        logger.debug("Parsing Mapping template");
        String currTemplate = currMappingObject.getString(CATALOG_JSON_MAPPING_TEMPLATE_ELEMENT_NAME);
        String currConfig = currMappingObject.getString(CATALOG_JSON_MAPPING_CONFIG_ELEMENT_NAME);
        saveResourceConfig(currConfig, rcRepo);
        saveTemplateConfig(currTemplate, ftRepo);
        logger.debug("Finished Parsing Mapping template");
    }

    /**
     * Saves the resource config to the database
     * @param configResourcePath the path to the resource config on the classpath
     * @param rcRepo the ResourceConfigRepository to use
     * @throws IOException thrown if the resource or template file on the classpath cannot be retrieved as an InputStream
     */
    private void saveResourceConfig(String configResourcePath, JpaRepository rcRepo) throws IOException{
        logger.debug("Saving Resource Config");
        String configName = getConfigResourceFileName(configResourcePath);
        ObjectMapper mapper = new ObjectMapper();
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.setResourceId(configName);
        InputStream in = getClassPathResourceAsInputStream(configResourcePath);
        resourceConfig.setConfig(mapper.readTree(IOUtils.toString(in, UTF_8)));
        rcRepo.save(resourceConfig);
        logger.debug("Finished Saving Resource Config");
    }

    /**
     * Saves the template config to the database
     * @param templateResourcePath the path to the template config on the classpath
     * @param ftRepo the FhirTemplateRepository to use
     * @throws IOException thrown if the resource or template file on the classpath cannot be retrieved as an InputStream
     */
    private void saveTemplateConfig(String templateResourcePath, JpaRepository ftRepo) throws IOException{
        logger.debug("Saving Template Config");
        String configName = getConfigResourceFileName(templateResourcePath);
        ObjectMapper mapper = new ObjectMapper();
        FhirTemplate fhirTemplate = new FhirTemplate();
        fhirTemplate.setTemplateId(configName);
        InputStream in = getClassPathResourceAsInputStream(templateResourcePath);
        fhirTemplate.setTemplate(mapper.readTree(IOUtils.toString(in, UTF_8)));
        ftRepo.save(fhirTemplate);
        logger.debug("Finished Saving Template Config");
    }

    /**
     * Retrives the config filename from the full path
     * @param configResourcePath the path to search
     * @return the name of the config file.
     */
    private String getConfigResourceFileName(String configResourcePath){
        //templates will be of the format resourceConfigs/dstu3_step-count.json
        //parse the config to get the
        logger.debug("Getting config file name from path");
        Matcher m = jsonFilePattern.matcher(configResourcePath);
        m.find();
        String configName = m.group(1);
        logger.debug("Config file name: " + configName);
        return configName;
    }

}
