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
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

@Configuration
public class InitDatabase {

    /*========================================================================*/
	/* CONSTANTS */
    /*========================================================================*/

    public static String RESOURCE_PATTERN = "resourceConfigs/*.json";
    public static String TEMPLATE_PATTERN= "fhirTemplates/*.json";
    public static String JSON_FILE_PATTERN = "(\\w+_[\\w-]+).json";

    /*========================================================================*/
	/* VARIABLES */
    /*========================================================================*/
    private static final Logger logger = LoggerFactory.getLogger(InitDatabase.class);
    private static Pattern jsonFilePattern = Pattern.compile(JSON_FILE_PATTERN);

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
            PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver(this.getClass().getClassLoader());
            logger.debug("Finding resource configs");
            Resource[] configResources = pathMatchingResourcePatternResolver.getResources(RESOURCE_PATTERN);
            logger.debug("Finding template configs");
            Resource[] templateResources = pathMatchingResourcePatternResolver.getResources(TEMPLATE_PATTERN);
            processResources(configResources, InitDatabase::saveResourceConfig, rcRepo);
            processResources(templateResources, InitDatabase::saveTemplateConfig, ftRepo);
            logger.debug("Finished loading mapping config files");
        };
    }
    private void processResources(Resource[] resources, BiConsumer<Resource, JpaRepository> saveConfig, JpaRepository repository){
        Arrays.asList(resources).forEach( resource -> {
            saveConfig.accept(resource, repository);
        });
    }

    /**
     * Saves the resource config to the database
     * @param configResource the config resource on the classpath
     * @param rcRepo the ResourceConfigRepository to use
     * @throws IOException thrown if the resource or template file on the classpath cannot be retrieved as an InputStream
     */
    private static void saveResourceConfig(Resource configResource, JpaRepository rcRepo){
        logger.debug("Saving Resource Config");
        String fileName = configResource.getFilename();
        logger.debug("Processing config: " + fileName);
        String configName = getConfigResourceFileName(fileName);
        ObjectMapper mapper = new ObjectMapper();
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.setResourceId(configName);
        try {
            InputStream in = configResource.getInputStream();
            resourceConfig.setConfig(mapper.readTree(IOUtils.toString(in, UTF_8)));
            rcRepo.save(resourceConfig);
        }
        catch(IOException ioe){
            logger.error("Could not read JSON config ", ioe);
        }
        logger.debug("Finished Saving Resource Config");
    }

    /**
     * Saves the template config to the database
     * @param templateResource the template resource on the classpath
     * @param ftRepo the FhirTemplateRepository to use
     * @throws IOException thrown if the resource or template file on the classpath cannot be retrieved as an InputStream
     */
    private static void saveTemplateConfig(Resource templateResource, JpaRepository ftRepo){
        logger.debug("Saving Template Config");
        String fileName = templateResource.getFilename();
        logger.debug("Processing template: " + fileName);
        String configName = getConfigResourceFileName(fileName);
        ObjectMapper mapper = new ObjectMapper();
        FhirTemplate fhirTemplate = new FhirTemplate();
        fhirTemplate.setTemplateId(configName);
        try{
            InputStream in = templateResource.getInputStream();
            fhirTemplate.setTemplate(mapper.readTree(IOUtils.toString(in, UTF_8)));
            ftRepo.save(fhirTemplate);
        }
        catch(IOException ioe){
            logger.error("Could not read JSON template ", ioe);
        }
        logger.debug("Finished Saving Template Config");
    }

    /**
     * Retrives the config filename from the full path
     * @param configResourcePath the path to search
     * @return the name of the config file.
     */
    private static String getConfigResourceFileName(String configResourcePath){
        //templates will be of the format dstu3_step-count.json
        //parse the config to get the
        logger.debug("Getting config file name from path");
        Matcher m = jsonFilePattern.matcher(configResourcePath);
        m.find();
        String configName = m.group(1);
        logger.debug("Config file name: " + configName);
        return configName;
    }
}
