package org.gtri.hdap.mdata.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.gtri.hdap.mdata.jpa.entity.ApplicationUser;
import org.gtri.hdap.mdata.jpa.repository.ApplicationUserRepository;
import org.gtri.hdap.mdata.service.ShimmerService;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Enumerations;
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
        ApplicationUser applicationUser = new ApplicationUser("123", "345", "fitbit");
        List<String> validQueryDates = createDateList("2018-06-01", "2018-07-01");

        given(applicationUserRepository.findByShimmerId("345")).willReturn(applicationUser);
        given(shimmerService.retrievePatientData(applicationUser, validQueryDates)).willReturn("789");

        ResultActions resultActions =
            mvc.perform(MockMvcRequestBuilders.get("/DocumentReference?subject=345&date=2018-06-01&date=2018-07-01"))
            .andExpect(status().isOk());
//        logger.debug(resultActions.toString());
//        .andExpect(content().json("{\"data\": \"activity data here\"}"));
        logger.debug("========== Entering getFindDocumentReference ==========");
    }


    @Test
    public void testGenerateDocumentReference() throws Exception{
        logger.debug("========== Entering testGenerateDocumentReference ==========");
        PatientDataController patientDataController = new PatientDataController();
        ApplicationUser applicationUser = new ApplicationUser("123", "456", "fitbit");
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