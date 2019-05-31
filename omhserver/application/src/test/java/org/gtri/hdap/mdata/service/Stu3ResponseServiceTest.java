package org.gtri.hdap.mdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.gtri.hdap.mdata.jpa.entity.FhirTemplate;
import org.gtri.hdap.mdata.jpa.entity.ResourceConfig;
import org.gtri.hdap.mdata.jpa.repository.ApplicationUserRepository;
import org.gtri.hdap.mdata.jpa.repository.FhirTemplateRepository;
import org.gtri.hdap.mdata.jpa.repository.ResourceConfigRepository;
import org.gtri.hdap.mdata.jpa.repository.ShimmerDataRepository;
import org.gtri.hdap.mdata.util.ShimmerUtil;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.*;

/**
 * Created by es130 on 9/14/2018.
 */
@RunWith(SpringRunner.class)
public class Stu3ResponseServiceTest {

    @TestConfiguration
    static class ResponseServiceTestContextConfiguration{
        @Bean
        public Stu3ResponseService responseService(){
            return new Stu3ResponseService();
        }
    }

    @Autowired
    private Stu3ResponseService stu3ResponseService;

    @MockBean
    private ApplicationUserRepository applicationUserRepository;
    @MockBean
    private ResourceConfigRepository resourceConfigRepository;
    @MockBean
    private FhirTemplateRepository fhirTemplateRepository;
    @MockBean
    private ShimmerDataRepository shimmerDataRepository;


    private Logger logger = LoggerFactory.getLogger(Stu3ResponseServiceTest.class);
    private ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, InputStream> configMap = new HashMap<>();
    private Map<String, InputStream> templatemap = new HashMap<>();

    @Before
    public void init() throws Exception{
        logger.debug("Reading in Observation config");
        loadMap(this.configMap, "resourceConfigs/*.json");
        loadMap(this.templatemap, "fhirTemplates/*.json");
        logger.debug("Finished initializing config");
    }
    private void loadMap(Map map, String resourcePath) throws Exception{
        logger.debug("Populating Map");
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver(this.getClass().getClassLoader());
        org.springframework.core.io.Resource[] resources = pathMatchingResourcePatternResolver.getResources(resourcePath);
        System.out.println("Resources: " + resources);
        for(int i=0; i<resources.length; i++){
            org.springframework.core.io.Resource resource = resources[i];
            logger.debug(resource.getFilename());
            map.put(resource.getFilename(), resource.getInputStream());
        }
        logger.debug("Finished populating map");
    }

    @Test
    public void testParseDate() throws Exception{
        logger.debug("========== Entering testParseDate==========");
        String expected = "2018-09-18T18:00:00.000Z";
        String nomillis = "2018-09-18T18:00:00Z";
        String notExpected = "2018-09-18";

        Date date = stu3ResponseService.parseDate(expected);
        assertTrue(date != null);
        date = stu3ResponseService.parseDate(nomillis);
        assertTrue(date != null);
        boolean hadError = false;
        try {
            date = stu3ResponseService.parseDate(notExpected);
        }
        catch(ParseException pe){
            hadError = true;
        }
        assertTrue(hadError == true);
        logger.debug("========== Exiting testParseDate==========");
    }

    @Test
    public void testGenerateDocumentReference() throws Exception{
        logger.debug("========== Entering testGenerateDocumentReference ==========");
        DocumentReference documentReference = stu3ResponseService.generateDocumentReference("doc123", "fitbit");

        assertTrue(documentReference != null);
        assertTrue(documentReference.getStatus() == Enumerations.DocumentReferenceStatus.CURRENT);

        logger.debug("Codeable concept: " + documentReference.getType().getText());
        assertTrue(documentReference.getType().getText().equals("OMH fitbit data"));
        assertTrue(documentReference.getIndexed() != null);
        assertTrue(documentReference.getContent().size() == 1);
        assertTrue(documentReference.getContent().get(0).getAttachment().getUrl().equals("Binary/doc123"));
        assertTrue(documentReference.getContent().get(0).getAttachment().getContentType().equals("application/json"));
        assertTrue(documentReference.getContent().get(0).getAttachment().getTitle().equals("OMH fitbit data"));
        assertTrue(documentReference.getContent().get(0).getAttachment().getCreation() != null);

        logger.debug("========== Exiting testGenerateDocumentReference ==========");
    }

    @Test
    public void testGenerateObservationStepCount() throws Exception{
        logger.debug("========== Entering testGenerateObservation ==========");
        //{
        //    "shim": "googlefit",
        //    "timeStamp": 1534251049,
        //    "body": [
        //    {
        //        "header": {
        //            "id": "3b9b68a2-e0fd-4bdd-ba85-4127a4e8bcee",
        //            "creation_date_time": "2018-08-14T12:50:49.383Z",
        //            "acquisition_provenance": {
        //                "source_name": "Google Fit API",
        //                "source_origin_id": "raw:com.google.step_count.cumulative:Google:Pixel 2 XL:5f9e1b9964be5834:LSM6DSM Step Counter"
        //            },
        //            "schema_id": {
        //                "namespace": "omh",
        //                "name": "step-count",
        //                "version": "2.0"
        //            }
        //        },
        //        "body": {
        //            "effective_time_frame": {
        //                "time_interval": {
        //                    "start_date_time": "2018-08-14T00:00:17.805Z",
        //                    "end_date_time": "2018-08-14T00:01:17.805Z"
        //                }
        //            },
        //            "step_count": 7
        //        }
        //    },
        // ...
        //    ]
        //}
        String sb = FileUtils.readFileToString(new File("src/test/resources/testomhresponse-stepcount.json"), UTF_8);
        ResourceConfig rc = new ResourceConfig();
        rc.setResourceId("stu3_step_count");

        InputStream configInputStream = this.configMap.get("stu3_step_count.json");
        assertNotNull(configInputStream);
        rc.setConfig(objectMapper.readTree(configInputStream));
        List<Resource> observationList = null;

        //create the fhir template
        FhirTemplate fhirTemplate = new FhirTemplate();
        fhirTemplate.setTemplateId("stu3_Observation");
        InputStream templateInputStream = this.templatemap.get("stu3_Observation.json");
        assertNotNull(templateInputStream);
        fhirTemplate.setTemplate(objectMapper.readTree(templateInputStream));

        given(fhirTemplateRepository.findOneByTemplateId(anyString()))
                .willReturn(fhirTemplate);
        observationList = stu3ResponseService.generateObservations(sb, rc);
        assertTrue(observationList.size() == 2);
        assertTrue(((Observation)observationList.get(0)).getValueQuantity().getValue().intValue() == 7 );
        assertTrue(((Observation)observationList.get(0)).getIdentifier().get(0).getSystem().equals(ShimmerUtil.PATIENT_IDENTIFIER_SYSTEM) );
        assertTrue(((Observation)observationList.get(0)).getIdentifier().get(0).getValue().equals("3b9b68a2-e0fd-4bdd-ba85-4127a4e8bcee") );
        assertTrue(((Observation)observationList.get(0)).getStatus().equals(Observation.ObservationStatus.UNKNOWN) );
        assertTrue(((Observation)observationList.get(0)).getCategory().get(0).getCoding().get(0).getCode().equals(ShimmerUtil.OBSERVATION_PHYSICAL_ACTIVITY_CATEGORY_CODE) );
        assertTrue(((Observation)observationList.get(0)).getCategory().get(0).getCoding().get(0).getSystem().equals(ShimmerUtil.OBSERVATION_CATEGORY_SYSTEM) );
        assertTrue(((Observation)observationList.get(0)).getCategory().get(0).getCoding().get(0).getDisplay().equals(ShimmerUtil.OBSERVATION_PHYSICAL_ACTIVITY_CATEGORY_DISPLAY) );
        assertTrue(((Observation)observationList.get(0)).getCode().getCoding().get(0).getCode().equals(ShimmerUtil.OBSERVATION_PHYSICAL_ACTIVITY_CODE_CODE) );
        assertTrue(((Observation)observationList.get(0)).getCode().getCoding().get(0).getSystem().equals(ShimmerUtil.OBSERVATION_CODE_SYSTEM) );
        assertTrue(((Observation)observationList.get(0)).getCode().getCoding().get(0).getDisplay().equals(ShimmerUtil.OBSERVATION_PHYSICAL_ACTIVITY_CODE_DISPLAY) );
        assertTrue(((Observation)observationList.get(0)).getEffectivePeriod().getStart() != null );
        assertTrue(((Observation)observationList.get(0)).getEffectivePeriod().getEnd() != null );
        assertTrue(((Observation)observationList.get(0)).getIssued() != null );

        assertTrue(((Observation)observationList.get(1)).getValueQuantity().getValue().intValue() == 27);
        assertTrue(((Observation)observationList.get(1)).getIdentifier().get(0).getSystem().equals(ShimmerUtil.PATIENT_IDENTIFIER_SYSTEM) );
        assertTrue(((Observation)observationList.get(1)).getIdentifier().get(0).getValue().equals("3b9b68a2-e0fd-4bdd-ba85-4127a4e8bcff") );
        assertTrue(((Observation)observationList.get(1)).getStatus().equals(Observation.ObservationStatus.UNKNOWN) );
        assertTrue(((Observation)observationList.get(1)).getCategory().get(0).getCoding().get(0).getCode().equals(ShimmerUtil.OBSERVATION_PHYSICAL_ACTIVITY_CATEGORY_CODE) );
        assertTrue(((Observation)observationList.get(1)).getCategory().get(0).getCoding().get(0).getSystem().equals(ShimmerUtil.OBSERVATION_CATEGORY_SYSTEM) );
        assertTrue(((Observation)observationList.get(1)).getCategory().get(0).getCoding().get(0).getDisplay().equals(ShimmerUtil.OBSERVATION_PHYSICAL_ACTIVITY_CATEGORY_DISPLAY) );
        assertTrue(((Observation)observationList.get(1)).getCode().getCoding().get(0).getCode().equals(ShimmerUtil.OBSERVATION_PHYSICAL_ACTIVITY_CODE_CODE) );
        assertTrue(((Observation)observationList.get(1)).getCode().getCoding().get(0).getSystem().equals(ShimmerUtil.OBSERVATION_CODE_SYSTEM) );
        assertTrue(((Observation)observationList.get(1)).getCode().getCoding().get(0).getDisplay().equals(ShimmerUtil.OBSERVATION_PHYSICAL_ACTIVITY_CODE_DISPLAY) );
        assertTrue(((Observation)observationList.get(1)).getEffectivePeriod().getStart() != null );
        assertTrue(((Observation)observationList.get(1)).getEffectivePeriod().getEnd() != null );
        assertTrue(((Observation)observationList.get(1)).getIssued() != null );
        logger.debug("========== Exiting testGenerateObservation ==========");
    }

    @Test
    public void testGenerateObservationHeartRate() throws Exception{
        logger.debug("========== Entering testGenerateObservation ==========");
        //{
        //    "shim": "googlefit",
        //    "timeStamp": 1534251049,
        //    "body": [
        //    {
        //        "header": {
        //            "id": "3b9b68a2-e0fd-4bdd-ba85-4127a4e8bcee",
        //            "creation_date_time": "2018-08-14T12:50:49.383Z",
        //            "acquisition_provenance": {
        //                "source_name": "Google Fit API",
        //                "source_origin_id": "raw:com.google.step_count.cumulative:Google:Pixel 2 XL:5f9e1b9964be5834:LSM6DSM Step Counter"
        //            },
        //            "schema_id": {
        //                "namespace": "omh",
        //                "name": "step-count",
        //                "version": "2.0"
        //            }
        //        },
//                "body": {
//                    "heart_rate": {
//                        "value": 50,
//                                "unit": "beats/min"
//                    },
//                    "effective_time_frame": {
//                        "date_time": "2013-02-05T07:25:00Z"
//                    },
//                    "temporal_relationship_to_physical_activity": "at rest",
//                            "user_notes": "I felt quite dizzy"
//                }
        //    },
        // ...
        //    ]
        //}
        String sb = FileUtils.readFileToString(new File("src/test/resources/testomhresponse-heartrate.json"), UTF_8);
        ResourceConfig rc = new ResourceConfig();
        rc.setResourceId("stu3_heart_rate");

        InputStream configInputStream = this.configMap.get("stu3_heart_rate.json");
        assertNotNull(configInputStream);
        rc.setConfig(objectMapper.readTree(configInputStream));
        List<Resource> observationList = null;

        //create the fhir template
        FhirTemplate fhirTemplate = new FhirTemplate();
        fhirTemplate.setTemplateId("stu3_Observation");
        InputStream templateInputStream = this.templatemap.get("stu3_Observation.json");
        assertNotNull(templateInputStream);
        fhirTemplate.setTemplate(objectMapper.readTree(templateInputStream));

        given(fhirTemplateRepository.findOneByTemplateId(anyString()))
                .willReturn(fhirTemplate);
        observationList = stu3ResponseService.generateObservations(sb, rc);
        assertTrue(observationList.size() == 2);
        assertTrue(((Observation)observationList.get(0)).getValueQuantity().getValue().intValue() == 50 );
        assertTrue(((Observation)observationList.get(0)).getIdentifier().get(0).getSystem().equals(ShimmerUtil.PATIENT_IDENTIFIER_SYSTEM) );
        assertTrue(((Observation)observationList.get(0)).getIdentifier().get(0).getValue().equals("3b9b68a2-e0fd-4bdd-ba85-4127a4e8gggg") );
        assertTrue(((Observation)observationList.get(0)).getStatus().equals(Observation.ObservationStatus.UNKNOWN) );
        assertTrue(((Observation)observationList.get(0)).getCategory().get(0).getCoding().get(0).getCode().equals(ShimmerUtil.OBSERVATION_HEART_RATE_CATEGORY_CODE) );
        assertTrue(((Observation)observationList.get(0)).getCategory().get(0).getCoding().get(0).getSystem().equals(ShimmerUtil.OBSERVATION_CATEGORY_SYSTEM) );
        assertTrue(((Observation)observationList.get(0)).getCategory().get(0).getCoding().get(0).getDisplay().equals(ShimmerUtil.OBSERVATION_HEART_RATE_CATEGORY_DISPLAY) );
        assertTrue(((Observation)observationList.get(0)).getCode().getCoding().get(0).getCode().equals(ShimmerUtil.OBSERVATION_HEART_RATE_CODE_CODE) );
        assertTrue(((Observation)observationList.get(0)).getCode().getCoding().get(0).getSystem().equals(ShimmerUtil.OBSERVATION_CODE_SYSTEM) );
        assertTrue(((Observation)observationList.get(0)).getCode().getCoding().get(0).getDisplay().equals(ShimmerUtil.OBSERVATION_HEART_RATE_CODE_DISPLAY) );
        assertTrue(((Observation)observationList.get(0)).getIssued() != null );

        assertTrue(((Observation)observationList.get(1)).getValueQuantity().getValue().intValue() == 40);
        assertTrue(((Observation)observationList.get(1)).getIdentifier().get(0).getSystem().equals(ShimmerUtil.PATIENT_IDENTIFIER_SYSTEM) );
        assertTrue(((Observation)observationList.get(1)).getIdentifier().get(0).getValue().equals("3b9b68a2-e0fd-4bdd-ba85-4127a4e8eeee") );
        assertTrue(((Observation)observationList.get(1)).getStatus().equals(Observation.ObservationStatus.UNKNOWN) );
        assertTrue(((Observation)observationList.get(1)).getCategory().get(0).getCoding().get(0).getCode().equals(ShimmerUtil.OBSERVATION_HEART_RATE_CATEGORY_CODE) );
        assertTrue(((Observation)observationList.get(1)).getCategory().get(0).getCoding().get(0).getSystem().equals(ShimmerUtil.OBSERVATION_CATEGORY_SYSTEM) );
        assertTrue(((Observation)observationList.get(1)).getCategory().get(0).getCoding().get(0).getDisplay().equals(ShimmerUtil.OBSERVATION_HEART_RATE_CATEGORY_DISPLAY) );
        assertTrue(((Observation)observationList.get(1)).getCode().getCoding().get(0).getCode().equals(ShimmerUtil.OBSERVATION_HEART_RATE_CODE_CODE) );
        assertTrue(((Observation)observationList.get(1)).getCode().getCoding().get(0).getSystem().equals(ShimmerUtil.OBSERVATION_CODE_SYSTEM) );
        assertTrue(((Observation)observationList.get(1)).getCode().getCoding().get(0).getDisplay().equals(ShimmerUtil.OBSERVATION_HEART_RATE_CODE_DISPLAY) );
        assertTrue(((Observation)observationList.get(1)).getIssued() != null );
        logger.debug("========== Exiting testGenerateObservation ==========");
    }
}
