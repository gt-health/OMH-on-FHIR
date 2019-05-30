package org.gtri.hdap.mdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertTrue;

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

    // FIXME
    @Ignore
    @Test
    public void testGenerateObservation() throws Exception{
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
        String sb = FileUtils.readFileToString(new File("src/test/resources/testomhresponse1.json"), UTF_8);
        ResourceConfig rc = new ResourceConfig();
        rc.setResourceId("stu3_step_count");
        rc.setConfig(objectMapper.readTree(FileUtils.readFileToString(new File("src/test/resources/testconfig1.json"), UTF_8)));
        List<Resource> observationList = stu3ResponseService.generateObservations(sb,rc);
        assertTrue(observationList.size() == 2);

        assertTrue(((Observation)observationList.get(0)).getValueQuantity().getValue().intValue() == 7 );

        assertTrue(((Observation)observationList.get(0)).getId() == null );
        assertTrue(((Observation)observationList.get(0)).getContained().get(0).getId().equals(ShimmerUtil.PATIENT_RESOURCE_ID) );
        assertTrue(((Observation)observationList.get(0)).getIdentifier().get(0).getSystem().equals(ShimmerUtil.PATIENT_IDENTIFIER_SYSTEM) );
        assertTrue(((Observation)observationList.get(0)).getIdentifier().get(0).getValue().equals("3b9b68a2-e0fd-4bdd-ba85-4127a4e8bcee") );
        assertTrue(((Observation)observationList.get(0)).getStatus().equals(Observation.ObservationStatus.UNKNOWN) );
        assertTrue(((Observation)observationList.get(0)).getCategory().get(0).getCoding().get(0).getCode().equals(ShimmerUtil.OBSERVATION_CATEGORY_CODE) );
        assertTrue(((Observation)observationList.get(0)).getCategory().get(0).getCoding().get(0).getSystem().equals(ShimmerUtil.OBSERVATION_CATEGORY_SYSTEM) );
        assertTrue(((Observation)observationList.get(0)).getCategory().get(0).getCoding().get(0).getDisplay().equals(ShimmerUtil.OBSERVATION_CATEGORY_DISPLAY) );
        assertTrue(((Observation)observationList.get(0)).getCode().getCoding().get(0).getCode().equals(ShimmerUtil.OBSERVATION_CODE_CODE) );
        assertTrue(((Observation)observationList.get(0)).getCode().getCoding().get(0).getSystem().equals(ShimmerUtil.OBSERVATION_CODE_SYSTEM) );
        assertTrue(((Observation)observationList.get(0)).getCode().getCoding().get(0).getDisplay().equals(ShimmerUtil.OBSERVATION_CODE_DISPLAY) );
        assertTrue(((Observation)observationList.get(0)).getSubject().getReference() != null );
        assertTrue(((Observation)observationList.get(0)).getEffectivePeriod().getStart() != null );
        assertTrue(((Observation)observationList.get(0)).getEffectivePeriod().getEnd() != null );
        assertTrue(((Observation)observationList.get(0)).getIssued() != null );
        assertTrue(((Observation)observationList.get(0)).getDevice().getDisplay().equals("some source,some step counter,1534251049") );

        assertTrue(((Observation)observationList.get(1)).getComponent().get(0).getValueQuantity().getValue().intValue() == 27);
        assertTrue(((Observation)observationList.get(1)).getId() != null );
        assertTrue(((Observation)observationList.get(1)).getContained().get(0).getId().equals(ShimmerUtil.PATIENT_RESOURCE_ID) );
        assertTrue(((Observation)observationList.get(1)).getIdentifier().get(0).getSystem().equals(ShimmerUtil.PATIENT_IDENTIFIER_SYSTEM) );
        assertTrue(((Observation)observationList.get(1)).getIdentifier().get(0).getValue().equals("3b9b68a2-e0fd-4bdd-ba85-4127a4e8bcff") );
        assertTrue(((Observation)observationList.get(1)).getStatus().equals(Observation.ObservationStatus.UNKNOWN) );
        assertTrue(((Observation)observationList.get(1)).getCategory().get(0).getCoding().get(0).getCode().equals(ShimmerUtil.OBSERVATION_CATEGORY_CODE) );
        assertTrue(((Observation)observationList.get(1)).getCategory().get(0).getCoding().get(0).getSystem().equals(ShimmerUtil.OBSERVATION_CATEGORY_SYSTEM) );
        assertTrue(((Observation)observationList.get(1)).getCategory().get(0).getCoding().get(0).getDisplay().equals(ShimmerUtil.OBSERVATION_CATEGORY_DISPLAY) );
        assertTrue(((Observation)observationList.get(1)).getCode().getCoding().get(0).getCode().equals(ShimmerUtil.OBSERVATION_CODE_CODE) );
        assertTrue(((Observation)observationList.get(1)).getCode().getCoding().get(0).getSystem().equals(ShimmerUtil.OBSERVATION_CODE_SYSTEM) );
        assertTrue(((Observation)observationList.get(1)).getCode().getCoding().get(0).getDisplay().equals(ShimmerUtil.OBSERVATION_CODE_DISPLAY) );
        assertTrue(((Observation)observationList.get(1)).getSubject().getReference() != null );
        assertTrue(((Observation)observationList.get(1)).getEffectivePeriod().getStart() != null );
        assertTrue(((Observation)observationList.get(1)).getEffectivePeriod().getEnd() != null );
        assertTrue(((Observation)observationList.get(1)).getIssued() != null );
        logger.debug("========== Exiting testGenerateObservation ==========");
    }
}
