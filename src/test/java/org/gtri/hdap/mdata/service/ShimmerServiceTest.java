package org.gtri.hdap.mdata.service;

import org.gtri.hdap.mdata.jpa.entity.ApplicationUser;
import org.gtri.hdap.mdata.jpa.repository.ApplicationUserRepository;
import org.gtri.hdap.mdata.jpa.repository.ShimmerDataRepository;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Created by es130 on 7/19/2018.
 */
@RunWith(SpringRunner.class)
public class ShimmerServiceTest {

    @TestConfiguration
    static class ShimmerServiceTestContextConfiguration{
        @Bean
        public ShimmerService shimmerService(){
            return new ShimmerService();
        }
    }

    @Autowired
    private ShimmerService shimmerService;
    @MockBean
    private ApplicationUserRepository applicationUserRepository;
    @MockBean
    private ShimmerDataRepository shimmerDataRepository;

    private Logger logger = LoggerFactory.getLogger(ShimmerServiceTest.class);


    @Test
    public void testParseDates() throws Exception{
        logger.debug("========== Entering testParseDates ==========");

        logger.debug("Testing valid date range");
        List<String> validQueryDates = createDateList("2018-06-01", "2018-07-01");
        Map<String, LocalDate> dateMap = shimmerService.parseDateQueries(validQueryDates);
        assertTrue(dateMap.get(ShimmerService.START_DATE_KEY).toString().equals("2018-06-01"));
        assertTrue(dateMap.get(ShimmerService.END_DATE_KEY).toString().equals("2018-07-01"));

        logger.debug("Testing valid date range; most recent first");
        validQueryDates = createDateList("2018-07-01", "2018-06-01");
        dateMap = shimmerService.parseDateQueries(validQueryDates);
        assertTrue(dateMap.get(ShimmerService.START_DATE_KEY).toString().equals("2018-06-01"));
        assertTrue(dateMap.get(ShimmerService.END_DATE_KEY).toString().equals("2018-07-01"));

        logger.debug("Testing valid date range with prefixes");
        validQueryDates = createDateList("ge2018-06-01", "le2018-07-01");
        dateMap = shimmerService.parseDateQueries(validQueryDates);
        assertTrue(dateMap.get(ShimmerService.START_DATE_KEY).toString().equals("2018-06-01"));
        assertTrue(dateMap.get(ShimmerService.END_DATE_KEY).toString().equals("2018-07-01"));

        logger.debug("Testing valid date range with start prefix");
        validQueryDates = createDateList("ge2018-06-01", "2018-07-01");
        dateMap = shimmerService.parseDateQueries(validQueryDates);
        assertTrue(dateMap.get(ShimmerService.START_DATE_KEY).toString().equals("2018-06-01"));
        assertTrue(dateMap.get(ShimmerService.END_DATE_KEY).toString().equals("2018-07-01"));

        logger.debug("Testing valid date range with end prefix");
        validQueryDates = createDateList("2018-06-01", "le2018-07-01");
        dateMap = shimmerService.parseDateQueries(validQueryDates);
        assertTrue(dateMap.get(ShimmerService.START_DATE_KEY).toString().equals("2018-06-01"));
        assertTrue(dateMap.get(ShimmerService.END_DATE_KEY).toString().equals("2018-07-01"));

        logger.debug("Testing valid date range with invalid start prefixes");
        validQueryDates = createDateList("le2018-06-01", "le2018-07-01");
        try {
            dateMap = shimmerService.parseDateQueries(validQueryDates);
        }
        catch(Exception e){
            assertTrue(e.getMessage().equals("Unsupported FHIR date prefix only GE is supported for start dates."));
        }

        logger.debug("Testing valid date range with invalid end prefixes");
        validQueryDates = createDateList("ge2018-06-01", "ge2018-07-01");
        try {
            dateMap = shimmerService.parseDateQueries(validQueryDates);
        }
        catch(Exception e){
            assertTrue(e.getMessage().equals("Unsupported FHIR date prefix only LE is supported for end dates."));
        }

        logger.debug("Testing valid date range with ONLY invalid start prefixes");
        validQueryDates = createDateList("le2018-06-01", "2018-07-01");
        try {
            dateMap = shimmerService.parseDateQueries(validQueryDates);
        }
        catch(Exception e){
            assertTrue(e.getMessage().equals("Unsupported FHIR date prefix only GE is supported for start dates."));
        }

        logger.debug("Testing valid date range with ONLY invalid end prefixes");
        validQueryDates = createDateList("2018-06-01", "ge2018-07-01");
        try {
            dateMap = shimmerService.parseDateQueries(validQueryDates);
        }
        catch(Exception e){
            assertTrue(e.getMessage().equals("Unsupported FHIR date prefix only LE is supported for end dates."));
        }

        logger.debug("Testing valid single date");
        validQueryDates = createDateList("2018-06-01", null);
        dateMap = shimmerService.parseDateQueries(validQueryDates);
        assertTrue(dateMap.get(ShimmerService.START_DATE_KEY).toString().equals("2018-06-01"));
        assertTrue(dateMap.get(ShimmerService.END_DATE_KEY).toString().equals("2018-06-01"));

        logger.debug("Testing valid single date with le prefix");
        validQueryDates = createDateList("le2018-06-01", null);
        dateMap = shimmerService.parseDateQueries(validQueryDates);
        assertTrue(dateMap.get(ShimmerService.START_DATE_KEY) == null);
        assertTrue(dateMap.get(ShimmerService.END_DATE_KEY).toString().equals("2018-06-01"));

        logger.debug("Testing valid single date with ge prefix");
        validQueryDates = createDateList("ge2018-06-01", null);
        dateMap = shimmerService.parseDateQueries(validQueryDates);
        assertTrue(dateMap.get(ShimmerService.START_DATE_KEY).toString().equals("2018-06-01"));
        assertTrue(dateMap.get(ShimmerService.END_DATE_KEY) == null);

        logger.debug("Testing valid date with invalid prefix");
        validQueryDates = createDateList("gt2018-07-01", null);
        try {
            dateMap = shimmerService.parseDateQueries(validQueryDates);
        }
        catch(Exception e){
            assertTrue(e.getMessage().equals("Unsupported FHIR date prefix only GE and LE are supported."));
        }

        logger.debug("Testing too many dates");
        validQueryDates = createDateList("2018-06-01", "ge2018-07-01");
        validQueryDates.addAll(createDateList("2018-08-01", "ge2018-09-01"));
        try {
            dateMap = shimmerService.parseDateQueries(validQueryDates);
        }
        catch(Exception e){
            assertTrue(e.getMessage().equals("No more than two dates can be passed as parameters."));
        }

        logger.debug("========== Exiting testParseDates ==========");
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
