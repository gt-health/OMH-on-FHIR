package org.gtri.hdap.mdata.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.JsonParser;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import org.apache.catalina.connector.Response;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.gtri.hdap.mdata.jpa.entity.ApplicationUser;
import org.gtri.hdap.mdata.jpa.entity.ShimmerData;
import org.gtri.hdap.mdata.jpa.repository.ApplicationUserRepository;
import org.gtri.hdap.mdata.jpa.repository.ShimmerDataRepository;
import org.gtri.hdap.mdata.service.ShimmerService;
import org.gtri.hdap.mdata.util.ShimmerUtil;
import org.hl7.fhir.dstu3.model.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.time.LocalDate;
import java.util.stream.Collectors;

/**
 * Created by es130 on 6/27/2018.
 */
@RestController
@SessionAttributes("shimmerId")
public class PatientDataController {

    /*========================================================================*/
    /* Variables */
    /*========================================================================*/
    private final Logger logger = LoggerFactory.getLogger(PatientDataController.class);

    @Autowired
    private ShimmerService shimmerService;
    @Autowired
    private ApplicationUserRepository applicationUserRepository;
    @Autowired
    private ShimmerDataRepository shimmerDataRepository;

    @ModelAttribute("shimmerId")
    public String shimmerId(){
        return "";
    }

    /*========================================================================*/
    /* Service Endpoint Methods */
    /*========================================================================*/
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
//    @RequestMapping(value="/shimmerAuthentication", method= RequestMethod.GET)
    //TODO: Do we really need to pass in the ModelMap
    @GetMapping("/shimmerAuthentication")
    public ModelAndView authenticateWithShimmer(ModelMap model,
                                                @ModelAttribute("shimmerId") String shimmerId,
                                                RedirectAttributes attributes,
                                                @RequestParam(name="ehrId", required=true) String ehrId,
                                                @RequestParam(name="shimkey", required=true) String shimkey,
                                                BindingResult bindingResult){
        logger.debug("Trying to connect to " + shimkey + " API");
        // Make a request to http://<shimmer-host>:8083/authorize/{shimKey}?username={userId}
        // The shimKey path parameter should be one of the keys listed below, e.g. fitbit
        // for example https://gt-apps.hdap.gatech.edu:8083/authorize/fitbit?username={userId}
        // The username query parameter can be set to any unique identifier you'd like to use to identify the user.

        String userShimmerId = getShimmerId(ehrId, shimkey);

        //see if the session attribute needs to be updated
        if(shimmerId.isEmpty()){
            model.addAttribute("shimmerId", userShimmerId);
        }

        String fitbitAuthUrl = null;
        try {
            fitbitAuthUrl = shimmerService.requestShimmerAuthUrl(userShimmerId, shimkey);
        }
        catch(Exception e){
            e.printStackTrace();
            //TODO redirect to no auth URL page
            //read this link http://www.baeldung.com/spring-boot-custom-error-page
        }

        logger.debug("Finished connection to " + shimkey + " API");

        //tell spring we want the attribute to survive the redirect
        attributes.addFlashAttribute("shimmerId", userShimmerId);
//        attributes.addAttribute("shimmerId", userShimmerId);

        String redirectUrl = "redirect:" + fitbitAuthUrl;
        logger.debug("Redirecting to " + redirectUrl);
        logger.debug("Model Keys: " + model.keySet());
        logger.debug("Model Values " + model.values());

        ModelAndView mvToReturn = new ModelAndView(redirectUrl, model);
        if(bindingResult.hasErrors()){
            logger.debug("Found Errors");
            bindingResult.getFieldErrors().forEach((FieldError fe) -> {
                logger.debug("Field: " + fe.getField() );
                logger.debug("Message: " + fe.getDefaultMessage());
                logger.debug("Bad Value: " + fe.getObjectName() + " " + fe.getRejectedValue());
            });
        }
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
    @GetMapping("/DocumentReference")
    public ResponseEntity findDocumentReference(@RequestParam(name="subject", required=true) String shimmerId,
                                                @RequestParam(name="date") List<String> dateQueries){
        logger.debug("processing document request");
        //look up the user
        ApplicationUser applicationUser = applicationUserRepository.findByShimmerId(shimmerId);
        String shimKey = applicationUser.getShimKey();

        String binaryRefId = "";
        //parse start and end dates
        try {
            //Query patient data
            binaryRefId = shimmerService.retrievePatientData(applicationUser, dateQueries);
        }
        catch(Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        logger.debug("Binary ID for patient data: " + binaryRefId);

        //generate the document reference
        DocumentReference documentReference = generateDocumentReference(binaryRefId, shimKey);

        logger.debug("finished processing document request");

        Bundle responseBundle = makeBundle(documentReference);

//        return ResponseEntity.ok(documentReference);
        return ResponseEntity.ok(responseBundle);
    }

    //handles requests of the format
    //GET https://apps.hdap.gatech.edu/hapiR4/baseR4/Binary?_id=EXexample
    @GetMapping(value = "/Binary/{documentId}",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody byte[] retrieveBinary(
                                 @RequestHeader("Accept") String acceptHeader,
                                 @PathVariable String documentId){
        logger.debug("Retrieving Binary with URL");
        byte[] docBytes = makeByteArrayForDocument(documentId);
        return docBytes;
    }

    @GetMapping("/Binary")
    public Bundle searchBinaryBundle(@RequestHeader("Accept") String acceptHeader,
                                     @RequestParam(name="_id", required = true)String documentId){
        logger.debug("Retriving Binary with URL param");
        return makeBundleForDocument(documentId);
    }

    //handles requests of the format
    //GET https://apps.hdap.gatech.edu/hapiR4/baseR4/Observation?subject=EXf201
    @GetMapping("/Observation")
    public Observation findObservation(@RequestParam(name="subject", required=true) String subjectId){
        return new Observation();
    }

    @RequestMapping("/authorize/{shimkey}/callback")
    public ModelAndView handleFitbitRedirect(ModelMap model,
                                       @ModelAttribute("shimmerId") String shimmerId,
                                       @PathVariable String shimkey,
                                       @RequestParam(name="code") String code,
                                       @RequestParam(name="state") String state){
        logger.debug("Handling successful Fitbit auth redirect");
        logger.debug("MODEL shimmer id " + model.get("shimmerId"));
        logger.debug("Passed in shimmer id " + shimmerId);
        logger.debug("Code " + code);
        logger.debug("State " + state);

        //TODO: YOU ARE HERE DO YOU NEED TO MAKE IT HANDLE A CALL BACK LIKE WHAT IS USED WITH POSTMAN?
        //"authorizationUrl": "https://www.fitbit.com/oauth2/authorize?response_type=code&client_id=22D3DR&
        // redirect_uri=https://apps.hdap.gatech.edu/mdata/authorize/fitbit/callback&scope=activity%20heartrate%20sleep%20weight&state=qdm1q7&prompt=login%20consent",

        //call made by fitbit to auth server
        //https://shimmer.apps.hdap.gatech.edu/authorize/fitbit/callback?code=bab6a0ebfaa5ff046bfc4e61e6461822be6d7f17&state=NVEcXD#_=_

        String omhOnFhirUi;

        //TODO: Why is this call to the shimmer API not working
        try {
            shimmerService.completeShimmerAuth(shimkey, code, state);
        }
        catch(Exception e){
            e.printStackTrace();
            omhOnFhirUi = "redirect:" + System.getenv(ShimmerUtil.OMH_ON_FHIR_LOGIN_ENV);
            model.addAttribute("loginSuccess", false);
            logger.debug("Error with Authentication. Redirecting to: " + omhOnFhirUi);
            return new ModelAndView(omhOnFhirUi, model);
        }

        omhOnFhirUi = "redirect:" + System.getenv(ShimmerUtil.OMH_ON_FHIR_CALLBACK_ENV);
        model.addAttribute("loginSuccess", true);
        model.addAttribute("shimmerId", shimmerId);
        logger.debug("Redirecting to: " + omhOnFhirUi);
        return new ModelAndView(omhOnFhirUi, model);
    }

    /*========================================================================*/
    /* Public Methods */
    /*========================================================================*/

    public DocumentReference generateDocumentReference(String documentId, String shimKey){
        String title = "OMH " + shimKey + " data";
        String binaryRef = "Binary/" + documentId;
        Date creationDate = new Date();
        //create a DocumentReference to return
        DocumentReference documentReference = new DocumentReference();
        documentReference.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.setText(title);
        documentReference.setType(codeableConcept);
        documentReference.setIndexed(creationDate);
        DocumentReference.DocumentReferenceContentComponent documentReferenceContentComponent = new DocumentReference.DocumentReferenceContentComponent();
        //create an Attachment that has a URL for the data
        Attachment attachment = new Attachment();
        attachment.setContentType("application/json");
        attachment.setUrl(binaryRef);
        attachment.setTitle(title);
        attachment.setCreation(creationDate);
        documentReferenceContentComponent.setAttachment(attachment);
        //add attachment to DocumentReference
        List<DocumentReference.DocumentReferenceContentComponent> documentContent = new ArrayList<DocumentReference.DocumentReferenceContentComponent>();
        documentContent.add(documentReferenceContentComponent);
        documentReference.setContent(documentContent);
        return documentReference;
    }

    /*========================================================================*/
    /* Private Methods */
    /*========================================================================*/

    private byte[] makeByteArrayForDocument(String documentId){
        logger.debug("Making Bundle for Document" + documentId);
        //get fitbit data for binary resource
        ShimmerData shimmerData = shimmerDataRepository.findByDocumentId(documentId);

        //get shimmer data
        String jsonData = shimmerData.getJsonData();

        logger.debug("Got JSON data " + jsonData);

        return jsonData.getBytes();
    }

    private Bundle makeBundleForDocument(String documentId){

        byte[] jsonData = makeByteArrayForDocument(documentId);

        //convert to base64
        byte[] base64EncodedData = Base64.getEncoder().encode(jsonData);

        //make the Binary object
        Binary binary = makeBinary(base64EncodedData);

        //convert to a bundle
        Bundle bundle = makeBundle(binary);

        //return the bundle
        return bundle;
    }

    private Binary makeBinary(byte[] base64EncodedData){
        Binary binary = new Binary();
        binary.setContentType("application/json");
        binary.setContent(base64EncodedData);
        return binary;
    }

    private Bundle makeBundle(Resource fhirResource){
        Bundle bundle = new Bundle();
        Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent();
        bundleEntryComponent.setResource(fhirResource);
        List<Bundle.BundleEntryComponent> bundleEntryComponentList = new ArrayList<Bundle.BundleEntryComponent>();
        bundleEntryComponentList.add(bundleEntryComponent);
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(bundleEntryComponentList.size());
        bundle.setEntry(bundleEntryComponentList);
        return bundle;
    }

    private String getShimmerId(String ehrId, String shimkey){
        logger.debug("Checking User");
        ApplicationUser user = applicationUserRepository.findByEhrId(ehrId);
        String shimmerId;
        if(user == null){
            //create a mapping
            logger.debug("User does not exist, creating");
            shimmerId = UUID.randomUUID().toString();
            ApplicationUser newUser = new ApplicationUser(ehrId, shimmerId, shimkey);
            applicationUserRepository.save(newUser);
            logger.debug("finished creating user");
        }
        else{
            logger.debug("Using existing user");
            shimmerId = user.getShimmerId();
        }
        return shimmerId;
    }

}
