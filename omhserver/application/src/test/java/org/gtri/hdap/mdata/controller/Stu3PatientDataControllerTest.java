package org.gtri.hdap.mdata.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.gtri.hdap.mdata.jpa.entity.ApplicationUser;
import org.gtri.hdap.mdata.jpa.entity.ApplicationUserId;
import org.gtri.hdap.mdata.jpa.repository.ApplicationUserRepository;
import org.gtri.hdap.mdata.service.ShimmerResponse;
import org.gtri.hdap.mdata.service.ShimmerService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

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
public class Stu3PatientDataControllerTest {

    private final Logger logger = LoggerFactory.getLogger(Stu3PatientDataControllerTest.class);

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
        ShimmerResponse shimmerResponse = new ShimmerResponse(HttpStatus.OK.value(), "{\"status\":" + HttpStatus.OK.value() + "}");
        ShimmerResponse failureResponse = new ShimmerResponse(HttpStatus.BAD_REQUEST.value(), "{\"status\":" + HttpStatus.BAD_REQUEST.value() + ", \"error\":\"Unsupported FHIR date prefix only GE is supported for start dates.\"}");

        given(applicationUserRepository.findByShimmerId(shimmerId)).willReturn(applicationUser);
        given(shimmerService.retrievePatientData(applicationUser, validQueryDates, "step_count")).willReturn(shimmerResponse);
        given(shimmerService.writePatientData(applicationUser, shimmerResponse)).willReturn(docId);

        mvc.perform(MockMvcRequestBuilders.get("/DocumentReference?subject=" + shimmerId + "&date=" + validDate1 + "&date=" + validDate2 + "&omhResource=step_count"))
        .andExpect(status().isOk());

        given(shimmerService.retrievePatientData(applicationUser, inValidQueryDates, "step_count")).willReturn(failureResponse);

        mvc.perform(MockMvcRequestBuilders.get("/DocumentReference?subject=" + shimmerId + "&date=" + inValidDate1 + "&date=" + inValidDate2 + "&omhResource=step_count"))
                .andExpect(status().is4xxClientError());

        logger.debug("========== Exiting getFindDocumentReference ==========");
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