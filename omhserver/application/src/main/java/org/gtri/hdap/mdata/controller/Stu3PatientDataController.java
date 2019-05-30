package org.gtri.hdap.mdata.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.gtri.hdap.mdata.jpa.entity.ApplicationUser;
import org.gtri.hdap.mdata.jpa.entity.ResourceConfig;
import org.gtri.hdap.mdata.jpa.repository.ApplicationUserRepository;
import org.gtri.hdap.mdata.jpa.repository.FhirTemplateRepository;
import org.gtri.hdap.mdata.jpa.repository.ResourceConfigRepository;
import org.gtri.hdap.mdata.service.Stu3ResponseService;
import org.gtri.hdap.mdata.service.ShimmerResponse;
import org.gtri.hdap.mdata.service.ShimmerService;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

/**
 * Created by es130 on 6/27/2018.
 */
@RestController
@CrossOrigin
@Api(value="patientdatacontroller", description="OMH on FHIR web service operations." )
public class Stu3PatientDataController {

    /*========================================================================*/
    /* Variables */
    /*========================================================================*/
    private final Logger logger = LoggerFactory.getLogger(Stu3PatientDataController.class);

    @Autowired
    private ShimmerService shimmerService;
    @Autowired
    private Stu3ResponseService stu3ResponseService;
    @Autowired
    private ApplicationUserRepository applicationUserRepository;
    @Autowired
    private ResourceConfigRepository resourceConfigRepository;
    @Autowired
    private FhirTemplateRepository fhirTemplateRepository;

    /*========================================================================*/
    /* Service Endpoint Methods */
    /*========================================================================*/
    @ApiOperation(value="View Spring Boot test.")
    @RequestMapping("/")
    public String index(){
        return "Greetings from Spring Boot!";
    }

    /**
     * Handles a Get request for a DocumentReference. It can take in up to two dates for a search between two
     * time periods.
     * @param shimmerId
     * @param dateQueries
     * @return
     */
    //GET https://apps.hdap.gatech.edu/hapiR4/baseR4/DocumentReference?subject=EXxcda
    @ApiOperation(value="Retrieve a DocumentReference for a subject with a specific Shimmer ID.")
    @GetMapping("/DocumentReference")
    public ResponseEntity findDocumentReference(
        @RequestParam(name="subject", required=true) String shimmerId,
        @RequestParam(name="date") List<String> dateQueries,
        @RequestParam(name="omhResource") String omhResource
    ){
        logger.debug("processing document request");
        //look up the user
        ApplicationUser applicationUser = applicationUserRepository.findByShimmerId(shimmerId);
        String shimKey = applicationUser.getApplicationUserId().getShimKey();

        String binaryRefId = "";
        //retrieve patient data
        ShimmerResponse shimmerResponse = shimmerService.retrievePatientData(applicationUser, dateQueries, omhResource);

        if(shimmerResponse.getResponseCode() == HttpStatus.OK.value()){
            binaryRefId = shimmerService.writePatientData(applicationUser, shimmerResponse);

            //generate the document reference
            DocumentReference documentReference = stu3ResponseService.generateDocumentReference(binaryRefId, shimKey);

            logger.debug("finished processing document request");

            Bundle responseBundle = stu3ResponseService.makeBundleWithSingleEntry(documentReference);
            return ResponseEntity.ok(responseBundle);
        }
        else{
            //not successful
            return ResponseEntity.status(shimmerResponse.getResponseCode()).body(shimmerResponse.getResponseData());
        }
    }

    //handles requests of the format
    //GET https://apps.hdap.gatech.edu/hapiR4/baseR4/Binary?_id=EXexample
    @ApiOperation(value="Retrieve a Binary with OMH data for a DocumentReference query.")
    @GetMapping(value = "/Binary/{documentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody byte[] retrieveBinary(
        @RequestHeader("Accept") String acceptHeader,
        @PathVariable String documentId
    ){
        logger.debug("Retrieving Binary with URL");
        byte[] docBytes = stu3ResponseService.makeByteArrayForDocument(documentId);
        return docBytes;
    }

    @ApiOperation(value="Retrieve a Binary with OMH data for a DocumentReference query.")
    @GetMapping("/Binary")
    public Bundle searchBinaryBundle(
        @RequestHeader("Accept") String acceptHeader,
        @RequestParam(name="_id", required = true)String documentId
    ) {
        logger.debug("Retriving Binary with URL param");
        return stu3ResponseService.makeBundleForDocument(documentId);
    }

    @ApiOperation(value="Retrieves Observation with OMH data using a resource type configuration.")
    @GetMapping(value="/Observation")
    public ResponseEntity<Bundle> findObservations(
        @RequestParam(name="subject", required=true) String shimmerId,
        @RequestParam(name="date") List<String> dateQueries,
        @RequestParam(name="omhResource") String omhResource,
        @RequestParam(name="fhirVersion", defaultValue = "stu3") String fhirVersion
    ) {
        //Get the config for step_count
        String resourceId = fhirVersion + "_" + omhResource;
        ResourceConfig resourceConfig = resourceConfigRepository.findOneByResourceId(resourceId);
        ApplicationUser applicationUser = applicationUserRepository.findByShimmerId(shimmerId);
        if (resourceConfig == null) {
            logger.error("Mapping config with ID: " + resourceId + " not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else if (applicationUser == null) {
            logger.error("Application user with ID: " + shimmerId + " not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        logger.debug("Processing observation request...");
        //look up the user
        String shimKey = applicationUser.getApplicationUserId().getShimKey();

        ShimmerResponse shimmerResponse;
        //parse start and end dates
        logger.debug("Printing out application user:");
        logger.debug(applicationUser.toString());
        logger.debug("Printing out date queries:");
        logger.debug(dateQueries.toString());
        shimmerResponse = shimmerService.retrieveShimmerData(
            ShimmerService.SHIMMER_ACTIVITY_RANGE_URL, applicationUser, dateQueries, omhResource
        );
        if( shimmerResponse.getResponseCode() != HttpStatus.OK.value()){
            logger.error("Shimmer service returned " + shimmerResponse.getResponseCode());
            logger.error(shimmerResponse.getResponseData());
            return ResponseEntity.status(shimmerResponse.getResponseCode()).build();
        }

        //generateObservationList
        List<Resource> observations;
        try {
            observations = stu3ResponseService.generateObservations(
                shimmerResponse.getResponseData(), resourceConfig
            );
        }
        catch(IOException ioe){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        return ResponseEntity.ok(stu3ResponseService.makeBundle(observations));
    }

}
