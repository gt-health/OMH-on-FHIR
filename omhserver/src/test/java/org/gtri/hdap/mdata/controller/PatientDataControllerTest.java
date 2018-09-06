package org.gtri.hdap.mdata.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.gtri.hdap.mdata.jpa.entity.ApplicationUser;
import org.gtri.hdap.mdata.jpa.entity.ApplicationUserId;
import org.gtri.hdap.mdata.jpa.repository.ApplicationUserRepository;
import org.gtri.hdap.mdata.service.ShimmerService;
import org.hl7.fhir.dstu3.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.*;

/**
 * Created by es130 on 6/27/2018.
 */

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:test.application.properties")
public class PatientDataControllerTest {

    private final Logger logger = LoggerFactory.getLogger(PatientDataControllerTest.class);

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ApplicationUserRepository applicationUserRepository;

    @MockBean
    private ShimmerService shimmerService;

    @Test
    public void getHello() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("Greetings from Spring Boot!")));
    }

    @Test
    public void getFindDocumentReference() throws Exception {
        logger.debug("========== Entering getFindDocumentReference ==========");
        //Mock the application repository and shimmer service
        String ehrId = "123";
        String shimmerId = "345";
        String shimkey = "fitbit";
        String docId = "789";
        String validDate1 = "2018-06-01";
        String validDate2 = "2018-07-01";
        String inValidDate1 = "le2018-06-01";
        String inValidDate2 = "ge2018-07-01";
        ApplicationUser applicationUser = new ApplicationUser(new ApplicationUserId(ehrId, shimkey), shimmerId);
        List<String> validQueryDates = createDateList(validDate1, validDate2);
        List<String> inValidQueryDates = createDateList(inValidDate1, inValidDate2);

        given(applicationUserRepository.findByShimmerId(shimmerId)).willReturn(applicationUser);
        given(shimmerService.retrievePatientData(applicationUser, validQueryDates)).willReturn(docId);

        mvc.perform(MockMvcRequestBuilders.get("/DocumentReference?subject=" + shimmerId + "&date=" + validDate1 + "&date=" + validDate2))
        .andExpect(status().isOk());
//            .andExpect(content().json("{\"data\": \"activity data here\"}"));

        given(shimmerService.retrievePatientData(applicationUser, inValidQueryDates)).willThrow(new Exception("Unsupported FHIR date prefix only GE is supported for start dates."));

        mvc.perform(MockMvcRequestBuilders.get("/DocumentReference?subject=" + shimmerId + "&date=" + inValidDate1 + "&date=" + inValidDate2))
                .andExpect(status().is4xxClientError());

        logger.debug("========== Entering getFindDocumentReference ==========");
    }


    @Test
    public void testGenerateDocumentReference() throws Exception{
        logger.debug("========== Entering testGenerateDocumentReference ==========");
        PatientDataController patientDataController = new PatientDataController();
        ApplicationUser applicationUser = new ApplicationUser(new ApplicationUserId("123", "fitbit"), "456");
        DocumentReference documentReference = patientDataController.generateDocumentReference("doc123", "fitbit");

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
    public void testGenerateObservation() throws Exception{
        logger.debug("========== Entering testGenerateObservation ==========");
        PatientDataController patientDataController = new PatientDataController();
        ApplicationUser applicationUser = new ApplicationUser(new ApplicationUserId("123", "fitbit"), "456");
        StringBuilder sb = new StringBuilder();
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
        sb.append("{")
            .append("\"shim\": \"googlefit\",")
            .append("\"timeStamp\": 1534251049,")
            .append("\"body\": [")
            .append("{")
            .append(    "\"header\": {")
            .append(        "\"id\": \"3b9b68a2-e0fd-4bdd-ba85-4127a4e8bcee\",")
            .append(        "\"creation_date_time\": \"2018-08-14T12:50:49.383Z\",")
            .append(        "\"acquisition_provenance\": {")
            .append(            "\"source_name\": \"some source\",")
            .append(            "\"source_origin_id\": \"some step counter\"")
            .append(        "},")
            .append(        "\"schema_id\": {")
            .append(            "\"namespace\": \"omh\",")
            .append(            "\"name\": \"step-count\",")
            .append(            "\"version\": \"2.0\"")
            .append(        "}")
            .append(    "},")
            .append(    "\"body\": {")
            .append(        "\"effective_time_frame\": {")
            .append(            "\"time_interval\": {")
            .append(                "\"start_date_time\": \"2018-08-14T00:00:17.805Z\",")
            .append(                "\"end_date_time\": \"2018-08-14T00:01:17.805Z\"")
            .append(            "}")
            .append(        "},")
            .append(        "\"step_count\": 7")
            .append(    "}")
            .append("},")
            .append("{")
            .append(    "\"header\": {")
            .append(        "\"id\": \"3b9b68a2-e0fd-4bdd-ba85-4127a4e8bcff\",")
            .append(        "\"creation_date_time\": \"2018-08-14T12:50:49.383Z\",")
            .append(        "\"acquisition_provenance\": {")
            .append(            "\"source_name\": \"some source\",")
            .append(            "\"source_origin_id\": \"some step counter\"")
            .append(        "},")
            .append(        "\"schema_id\": {")
            .append(            "\"namespace\": \"omh\",")
            .append(            "\"name\": \"step-count\",")
            .append(            "\"version\": \"2.0\"")
            .append(        "}")
            .append(    "},")
            .append(    "\"body\": {")
            .append(        "\"effective_time_frame\": {")
            .append(            "\"time_interval\": {")
            .append(                "\"start_date_time\": \"2018-08-14T00:02:17.805Z\",")
            .append(                "\"end_date_time\": \"2018-08-14T00:03:17.805Z\"")
            .append(            "}")
            .append(        "},")
            .append(        "\"step_count\": 27")
            .append(    "}")
            .append("}")
            .append("]")
        .append("}");
        List<Resource> observationList = patientDataController.generateObservationList("123456", sb.toString());
        assertTrue(observationList.size() == 2);

        assertTrue(((Observation)observationList.get(0)).getComponent().get(0).getValueQuantity().getValue().intValue() == 7 );
        assertTrue(((Observation)observationList.get(0)).getId() != null );
        assertTrue(((Observation)observationList.get(0)).getContained().get(0).getId().equals(PatientDataController.PATIENT_RESOURCE_ID) );
        assertTrue(((Observation)observationList.get(0)).getIdentifier().get(0).getSystem().equals(PatientDataController.PATIENT_IDENTIFIER_SYSTEM) );
        assertTrue(((Observation)observationList.get(0)).getIdentifier().get(0).getValue().equals("3b9b68a2-e0fd-4bdd-ba85-4127a4e8bcee") );
        assertTrue(((Observation)observationList.get(0)).getStatus().equals(Observation.ObservationStatus.UNKNOWN) );
        assertTrue(((Observation)observationList.get(0)).getCategory().get(0).getCoding().get(0).getCode().equals(PatientDataController.OBSERVATION_CATEGORY_CODE) );
        assertTrue(((Observation)observationList.get(0)).getCategory().get(0).getCoding().get(0).getSystem().equals(PatientDataController.OBSERVATION_CATEGORY_SYSTEM) );
        assertTrue(((Observation)observationList.get(0)).getCategory().get(0).getCoding().get(0).getDisplay().equals(PatientDataController.OBSERVATION_CATEGORY_DISPLAY) );
        assertTrue(((Observation)observationList.get(0)).getCode().getCoding().get(0).getCode().equals(PatientDataController.OBSERVATION_CODE_CODE) );
        assertTrue(((Observation)observationList.get(0)).getCode().getCoding().get(0).getSystem().equals(PatientDataController.OBSERVATION_CODE_SYSTEM) );
        assertTrue(((Observation)observationList.get(0)).getCode().getCoding().get(0).getDisplay().equals(PatientDataController.OBSERVATION_CODE_DISPLAY) );
        assertTrue(((Observation)observationList.get(0)).getSubject().getReference() != null );
        assertTrue(((Observation)observationList.get(0)).getEffectivePeriod().getStart() != null );
        assertTrue(((Observation)observationList.get(0)).getEffectivePeriod().getEnd() != null );
        assertTrue(((Observation)observationList.get(0)).getIssued() != null );
        assertTrue(((Observation)observationList.get(0)).getDevice().getDisplay().equals("some source,some step counter,1534251049") );

        assertTrue(((Observation)observationList.get(1)).getComponent().get(0).getValueQuantity().getValue().intValue() == 27);
        assertTrue(((Observation)observationList.get(1)).getId() != null );
        assertTrue(((Observation)observationList.get(1)).getContained().get(0).getId().equals(PatientDataController.PATIENT_RESOURCE_ID) );
        assertTrue(((Observation)observationList.get(1)).getIdentifier().get(0).getSystem().equals(PatientDataController.PATIENT_IDENTIFIER_SYSTEM) );
        assertTrue(((Observation)observationList.get(1)).getIdentifier().get(0).getValue().equals("3b9b68a2-e0fd-4bdd-ba85-4127a4e8bcff") );
        assertTrue(((Observation)observationList.get(1)).getStatus().equals(Observation.ObservationStatus.UNKNOWN) );
        assertTrue(((Observation)observationList.get(1)).getCategory().get(0).getCoding().get(0).getCode().equals(PatientDataController.OBSERVATION_CATEGORY_CODE) );
        assertTrue(((Observation)observationList.get(1)).getCategory().get(0).getCoding().get(0).getSystem().equals(PatientDataController.OBSERVATION_CATEGORY_SYSTEM) );
        assertTrue(((Observation)observationList.get(1)).getCategory().get(0).getCoding().get(0).getDisplay().equals(PatientDataController.OBSERVATION_CATEGORY_DISPLAY) );
        assertTrue(((Observation)observationList.get(1)).getCode().getCoding().get(0).getCode().equals(PatientDataController.OBSERVATION_CODE_CODE) );
        assertTrue(((Observation)observationList.get(1)).getCode().getCoding().get(0).getSystem().equals(PatientDataController.OBSERVATION_CODE_SYSTEM) );
        assertTrue(((Observation)observationList.get(1)).getCode().getCoding().get(0).getDisplay().equals(PatientDataController.OBSERVATION_CODE_DISPLAY) );
        assertTrue(((Observation)observationList.get(1)).getSubject().getReference() != null );
        assertTrue(((Observation)observationList.get(1)).getEffectivePeriod().getStart() != null );
        assertTrue(((Observation)observationList.get(1)).getEffectivePeriod().getEnd() != null );
        assertTrue(((Observation)observationList.get(1)).getIssued() != null );
        logger.debug("========== Exiting testGenerateObservation ==========");
    }

    private List<String> createDateList(String startDate, String endDate){
        List<String> validQueryDates = new ArrayList<String>();
        if(startDate != null) {
            validQueryDates.add(startDate);
        }
        if(endDate != null) {
            validQueryDates.add(endDate);
        }
        return validQueryDates;
    }

}