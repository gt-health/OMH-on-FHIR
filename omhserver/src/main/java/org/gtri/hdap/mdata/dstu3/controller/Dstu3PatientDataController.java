package org.gtri.hdap.mdata.dstu3.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.gtri.hdap.mdata.common.jpa.entity.ApplicationUser;
import org.gtri.hdap.mdata.common.jpa.entity.ResourceConfig;
import org.gtri.hdap.mdata.common.jpa.repository.ApplicationUserRepository;
import org.gtri.hdap.mdata.common.jpa.repository.FhirTemplateRepository;
import org.gtri.hdap.mdata.common.jpa.repository.ResourceConfigRepository;
import org.gtri.hdap.mdata.common.jpa.repository.ShimmerDataRepository;
import org.gtri.hdap.mdata.dstu3.service.Dstu3ResponseService;
import org.gtri.hdap.mdata.common.service.ShimmerResponse;
import org.gtri.hdap.mdata.common.service.ShimmerService;
import org.gtri.hdap.mdata.common.util.ShimmerUtil;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.*;

/**
 * Created by es130 on 6/27/2018.
 */
@RestController
@SessionAttributes("shimmerId")
@Api(value="patientdatacontroller", description="OMH on FHIR web service operations." )
public class Dstu3PatientDataController {

    /*========================================================================*/
    /* Variables */
    /*========================================================================*/
    private final Logger logger = LoggerFactory.getLogger(Dstu3PatientDataController.class);

    @Autowired
    private ShimmerService shimmerService;
    @Autowired
    private Dstu3ResponseService dstu3ResponseService;
    @Autowired
    private ApplicationUserRepository applicationUserRepository;
    @Autowired
    private ShimmerDataRepository shimmerDataRepository;
    @Autowired
    private ResourceConfigRepository resourceConfigRepository;
    @Autowired
    private FhirTemplateRepository fhirTemplateRepository;


    @ModelAttribute("shimmerId")
    public String shimmerId(){
        return "";
    }

    /*========================================================================*/
    /* Service Endpoint Methods */
    /*========================================================================*/
    @ApiOperation(value="View Spring Boot test.")
    @RequestMapping("/")
    public String index(){
        return "Greetings from Spring Boot!";
    }

    /**
     * Make a request to http://<shimmer-host>:8083/authorize/{shimKey}?username={userId}
     * @param model
     * @param ehrId the ID of the patient in the EHR
     * @param shimkey the ID for the patient in Shimmer
     * @return
     */
    @ApiOperation(value="Authenticate an EHR user to use a specific shim with the Shimmer library.")
    @GetMapping("/shimmerAuthentication")
    public ModelAndView authenticateWithShimmer(ModelMap model,
        @ModelAttribute("shimmerId") String shimmerId,
        RedirectAttributes attributes,
        @RequestParam(name="ehrId", required=true) String ehrId,
        @RequestParam(name="shimkey", required=true) String shimkey,
        BindingResult bindingResult
    ){
        logger.debug("Entering method authenticateWithShimmer");
        logger.debug("Trying to connect to " + shimkey + " API");
        // Make a request to http://<shimmer-host>:8083/authorize/{shimKey}?username={userId}
        // The shimKey path parameter should be one of the keys listed below, e.g. fitbit
        // for example https://gt-apps.hdap.gatech.edu:8083/authorize/fitbit?username={userId}
        // The username query parameter can be set to any unique identifier you'd like to use to identify the user.

        logger.debug("calling getShimmerId");
        String userShimmerId = dstu3ResponseService.getShimmerId(ehrId, shimkey);
        logger.debug("called getShimmerId");
        //add the shimmer id to the model
        model.addAttribute("shimmerId", userShimmerId);

        ShimmerResponse shimmerResponse = shimmerService.requestShimmerAuthUrl(userShimmerId, shimkey);
        String oauthAuthUrl = null;
        if( shimmerResponse.getResponseCode() == HttpStatus.OK.value()){
            oauthAuthUrl = shimmerResponse.getResponseData();
        }
        else{
            //did not get a response URL
            String callbackUrl= "redirect:" + System.getenv(ShimmerUtil.OMH_ON_FHIR_CALLBACK_ENV);
            model.addAttribute("loginSuccess", false);
            logger.debug("HTTP " + shimmerResponse.getResponseCode() + " returned by Shimmer Auth response. Could not authorize shimmer user " +
                    shimmerResponse.getResponseData() + "Error with Authentication. Redirecting to: " + callbackUrl);
            return new ModelAndView(callbackUrl, model);
        }

        logger.debug("Finished connection to " + shimkey + " API");

        //tell spring we want the attribute to survive the redirect
        attributes.addFlashAttribute("shimmerId", userShimmerId);

        //If the returned oauthAuthUrl equals the final callback URL for UI then the user has already
        //linked the EHR user to their device account via shimmer. Update the model to contain
        //loginSuccess true. The shimmerID for the model was set above so no need to set it again.
        if( oauthAuthUrl.equals(System.getenv(ShimmerUtil.OMH_ON_FHIR_CALLBACK_ENV))) {
            logger.debug("User already approved. Forwarding to login callback UI page");
            model.addAttribute("loginSuccess", true);
        }

        String redirectUrl = "redirect:" + oauthAuthUrl;
        //THIS IS A HACK, check and remove  org.springframework.validation.BindingResult.shimmerId from the model
        model.remove("org.springframework.validation.BindingResult.shimmerId");

        ModelAndView mvToReturn = new ModelAndView(redirectUrl, model);

        return mvToReturn;
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
        @RequestParam(name="date") List<String> dateQueries
    ){
        logger.debug("processing document request");
        //look up the user
        ApplicationUser applicationUser = applicationUserRepository.findByShimmerId(shimmerId);
        String shimKey = applicationUser.getApplicationUserId().getShimKey();

        String binaryRefId = "";
        //retrieve patient data
        ShimmerResponse shimmerResponse = shimmerService.retrievePatientData(applicationUser, dateQueries);

        if(shimmerResponse.getResponseCode() == HttpStatus.OK.value()){
            binaryRefId = shimmerService.writePatientData(applicationUser, shimmerResponse);

            //generate the document reference
            DocumentReference documentReference = dstu3ResponseService.generateDocumentReference(binaryRefId, shimKey);

            logger.debug("finished processing document request");

            Bundle responseBundle = dstu3ResponseService.makeBundleWithSingleEntry(documentReference);
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
        byte[] docBytes = dstu3ResponseService.makeByteArrayForDocument(documentId);
        return docBytes;
    }

    @ApiOperation(value="Retrieve a Binary with OMH data for a DocumentReference query.")
    @GetMapping("/Binary")
    public Bundle searchBinaryBundle(
        @RequestHeader("Accept") String acceptHeader,
        @RequestParam(name="_id", required = true)String documentId
    ) {
        logger.debug("Retriving Binary with URL param");
        return dstu3ResponseService.makeBundleForDocument(documentId);
    }

    @ApiOperation(value="Retrieves Observation with OMH data using a resource type configuration.")
    @GetMapping(value="/Observation", consumes={"application/json"})
    public ResponseEntity<Bundle> findObservations(
        @RequestParam(name="subject", required=true) String shimmerId,
        @RequestParam(name="date") List<String> dateQueries,
        @RequestParam(name="omhResource") String omhResource,
        @RequestParam(name="fhirVersion", defaultValue = "dstu3") String fhirVersion
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
            observations = dstu3ResponseService.generateObservations(
                shimmerResponse.getResponseData(), resourceConfig
            );
        }
        catch(IOException ioe){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        return ResponseEntity.ok(dstu3ResponseService.makeBundle(observations));
    }

    @ApiOperation(value="Callback method for Shimmer to use during user authentication.")
    @GetMapping("/authorize/{shimkey}/callback")
    public ModelAndView handleShimmerOauthCallback(ModelMap model,
       @ModelAttribute("shimmerId") String shimmerId,
       @PathVariable String shimkey,
       @RequestParam(name="code") String code,
       @RequestParam(name="state") String state
    ){
        logger.debug("Handling successful " + shimkey + " auth redirect");
        logger.debug("MODEL shimmer id " + model.get("shimmerId"));
        logger.debug("Passed in shimmer id " + shimmerId);
        logger.debug("Code " + code);
        logger.debug("State " + state);

        String omhOnFhirUi;

        //TODO: Why is this call to the shimmer API not working for Fitbit?
        try {
            shimmerService.completeShimmerAuth(shimkey, code, state);
        }
        catch(Exception e){
            e.printStackTrace();
            omhOnFhirUi = "redirect:" + System.getenv(ShimmerUtil.OMH_ON_FHIR_CALLBACK_ENV);
            model.addAttribute("loginSuccess", false);
            logger.debug("Error with Authentication. Redirecting to: " + omhOnFhirUi);
            return new ModelAndView(omhOnFhirUi, model);
        }

        ApplicationUser applicationUser = applicationUserRepository.findByShimmerId(shimmerId);
        if(applicationUser != null){
            applicationUserRepository.save(applicationUser);
        }
        else{
            omhOnFhirUi = "redirect:" + System.getenv(ShimmerUtil.OMH_ON_FHIR_CALLBACK_ENV);
            model.addAttribute("loginSuccess", false);
            logger.debug("Could not find Shimmer ID for user. Redirecting to: " + omhOnFhirUi);
            return new ModelAndView(omhOnFhirUi, model);
        }

        omhOnFhirUi = "redirect:" + System.getenv(ShimmerUtil.OMH_ON_FHIR_CALLBACK_ENV);
        model.addAttribute("loginSuccess", true);
        model.addAttribute("shimmerId", shimmerId);
        logger.debug("Redirecting to: " + omhOnFhirUi);
        return new ModelAndView(omhOnFhirUi, model);
    }
}
