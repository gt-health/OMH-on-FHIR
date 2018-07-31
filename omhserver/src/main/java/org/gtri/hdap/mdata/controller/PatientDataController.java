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
import org.hl7.fhir.dstu3.model.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.*;
import java.time.LocalDate;
import java.util.stream.Collectors;

/**
 * Created by es130 on 6/27/2018.
 */
@RestController
public class PatientDataController {

    /*========================================================================*/
    /* Constants */
    /*========================================================================*/
    public static String OMH_ON_FHIR_CALLBACK_ENV = "OMH_ON_FHIR_CALLBACK";
    public static String OMH_ON_FHIR_LOGIN_ENV = "OMH_ON_FHIR_LOGIN";

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
                                                RedirectAttributes redirectAttributes,
                                                @RequestParam(name="ehrId", required=true) String ehrId,
                                                @RequestParam(name="shimkey", required=true) String shimkey){
        logger.debug("Trying to connect to " + shimkey + " API");
        // Make a request to http://<shimmer-host>:8083/authorize/{shimKey}?username={userId}
        // The shimKey path parameter should be one of the keys listed below, e.g. fitbit
        // for example https://gt-apps.hdap.gatech.edu:8083/authorize/fitbit?username={userId}
        // The username query parameter can be set to any unique identifier you'd like to use to identify the user.

        String shimmerId = getShimmerId(ehrId, shimkey);

        String fitbitAuthUrl = null;
        try {
            fitbitAuthUrl = shimmerService.requestShimmerAuthUrl(shimmerId, shimkey);
        }
        catch(Exception e){
            e.printStackTrace();
            //TODO redirect to no auth URL page
            //read this link http://www.baeldung.com/spring-boot-custom-error-page
        }

        logger.debug("Finished connection to " + shimkey + " API");
        model.addAttribute("shimmerId", shimmerId);
        redirectAttributes.addFlashAttribute("flashShimmerId", shimmerId);
        redirectAttributes.addAttribute("shimmerId", shimmerId);

        String redirectUrl = "redirect:" + fitbitAuthUrl;
        return new ModelAndView(redirectUrl, model);
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

//        logger.debug("Encoding DocumentReference to JSON");
//        IParser jsonParser = FhirContext.forDstu3().newJsonParser();
//        String docJson = jsonParser.encodeResourceToString(documentReference);
//        logger.debug("Encoded JSON");
//        logger.debug(docJson);
        return ResponseEntity.ok(documentReference);
//        return ResponseEntity.ok(docJson);
    }

    //handles requests of the format
    //GET https://apps.hdap.gatech.edu/hapiR4/baseR4/Binary?_id=EXexample
    @GetMapping("/Binary/{documentId}")
    public Bundle retrieveBinary(@PathVariable String documentId){
        return makeBundleForDocument(documentId);
    }

    @GetMapping("/Binary")
    public Bundle retrieveBinaryFile(@RequestParam(name="_id", required = true)String documentId){
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
        logger.debug("Passing in shimmer id " + shimmerId);

        String omhOnFhirUi;

        //TODO: Why is this call to the shimmer API not working
//        try {
//            shimmerService.completeShimmerAuth(shimkey, code, state);
//        }
//        catch(Exception e){
//            e.printStackTrace();
//            omhOnFhirUi = "redirect:" + System.getenv(OMH_ON_FHIR_LOGIN_ENV);
//            model.addAttribute("loginSuccess", false);
//            logger.debug("Error with Authentication. Redirecting to: " + omhOnFhirUi);
//            return new ModelAndView(omhOnFhirUi, model);
//        }

        omhOnFhirUi = "redirect:" + System.getenv(OMH_ON_FHIR_CALLBACK_ENV);
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

    private Bundle makeBundleForDocument(String documentId){
        //get fitbit data for binary resource
        ShimmerData shimmerData = shimmerDataRepository.findByDocumentId(documentId);

        //get shimmer data
        String jsonData = shimmerData.getJsonData();

        //convert to base64
        byte[] base64EncodedData = Base64.getEncoder().encode(jsonData.getBytes());

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
