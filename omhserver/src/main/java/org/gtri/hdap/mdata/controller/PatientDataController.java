package org.gtri.hdap.mdata.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gtri.hdap.mdata.jpa.entity.ApplicationUser;
import org.gtri.hdap.mdata.jpa.entity.ApplicationUserId;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by es130 on 6/27/2018.
 */
@RestController
@SessionAttributes("shimmerId")
public class PatientDataController {

    public static String PATIENT_RESOURCE_ID = "p";
    public static String PATIENT_IDENTIFIER_SYSTEM = "https://omh.org/shimmer/patient_ids";
    public static String OBSERVATION_CATEGORY_SYSTEM = "https://snomed.info.sct";
    public static String OBSERVATION_CATEGORY_CODE = "68130003";
    public static String OBSERVATION_CATEGORY_DISPLAY = "Physical activity (observable entity)";
    public static String OBSERVATION_CODE_SYSTEM = "http://loinc.org";
    public static String OBSERVATION_CODE_CODE = "55423-8";
    public static String OBSERVATION_CODE_DISPLAY = "Number of steps in unspecified time Pedometer";
    public static String OBSERVATION_COMPONENT_CODE_SYSTEM = "http://hl7.org/fhir/observation-statistics";
    public static String OBSERVATION_COMPONENT_CODE_CODE = "maximum";
    public static String OBSERVATION_COMPONENT_CODE_DISPLAY = "Maximum";
    public static String OBSERVATION_COMPONENT_CODE_TEXT = "Maximum";
    public static String OBSERVATION_COMPONENT_VALUE_CODE_UNIT = "/{tot}";
    public static String OBSERVATION_COMPONENT_VALUE_CODE_SYSTEM = "http://unitsofmeasure.org";
    public static String OBSERVATION_COMPONENT_VALUE_CODE_CODE = "{steps}/{tot}";

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

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

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

        String oauthAuthUrl = null;
        try {
            oauthAuthUrl = shimmerService.requestShimmerAuthUrl(userShimmerId, shimkey);
        }
        catch(Exception e){
            e.printStackTrace();
            //TODO redirect to no auth URL page
            //read this link http://www.baeldung.com/spring-boot-custom-error-page
        }

        logger.debug("Finished connection to " + shimkey + " API");

        //tell spring we want the attribute to survive the redirect
        attributes.addFlashAttribute("shimmerId", userShimmerId);

        String redirectUrl = "redirect:" + oauthAuthUrl;
        logger.debug("Redirecting to " + redirectUrl);
        logger.debug("Model Keys: " + model.keySet());
        logger.debug("Model Values " + model.values());

        //THIS IS A HACK, check and remove  org.springframework.validation.BindingResult.shimmerId from the model
        model.remove("org.springframework.validation.BindingResult.shimmerId");

        logger.debug("Searched model for BindingResult to remove");
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
        String shimKey = applicationUser.getApplicationUserId().getShimKey();

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

        Bundle responseBundle = makeBundleWithSingleEntry(documentReference);

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
    public ResponseEntity findObservation(@RequestParam(name="subject", required=true) String shimmerId,
                                       @RequestParam(name="date") List<String> dateQueries){

        logger.debug("processing observation request");
        //look up the user
        ApplicationUser applicationUser = applicationUserRepository.findByShimmerId(shimmerId);
        String shimKey = applicationUser.getApplicationUserId().getShimKey();

        String jsonResponse = "";
        //parse start and end dates
        try {
            //Query patient data
            jsonResponse = shimmerService.retrieveShimmerData(applicationUser, dateQueries);
        }
        catch(Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        //generateObservationList
        List<Resource> observations;
        try {
            observations = generateObservationList(shimKey, jsonResponse);
        }
        catch(IOException ioe){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not generate observation.");
        }

        Bundle responseBundle = makeBundle(observations);
        return ResponseEntity.ok(responseBundle);
    }

    @RequestMapping("/authorize/{shimkey}/callback")
    public ModelAndView handleShimmerOauthCallback(ModelMap model,
                                       @ModelAttribute("shimmerId") String shimmerId,
                                       @PathVariable String shimkey,
                                       @RequestParam(name="code") String code,
                                       @RequestParam(name="state") String state){
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
            omhOnFhirUi = "redirect:" + System.getenv(ShimmerUtil.OMH_ON_FHIR_LOGIN_ENV);
            model.addAttribute("loginSuccess", false);
            logger.debug("Error with Authentication. Redirecting to: " + omhOnFhirUi);
            return new ModelAndView(omhOnFhirUi, model);
        }

        ApplicationUser applicationUser = applicationUserRepository.findByShimmerId(shimmerId);
        applicationUser.setLoggedIn(true);
        applicationUserRepository.save(applicationUser);

        omhOnFhirUi = "redirect:" + System.getenv(ShimmerUtil.OMH_ON_FHIR_CALLBACK_ENV);
        model.addAttribute("loginSuccess", true);
        model.addAttribute("shimmerId", shimmerId);
        logger.debug("Redirecting to: " + omhOnFhirUi);
        return new ModelAndView(omhOnFhirUi, model);
    }
//
//    @GetMapping("/loginStatus/{shimmerId}")
//    public ResponseEntity loginStatus(@PathVariable String shimmerId){
//        ApplicationUser applicationUser = applicationUserRepository.findByShimmerId(shimmerId);
//        return ResponseEntity.ok(applicationUser.getLoggedIn());
//    }
//
//    @GetMapping("/logout/{shimmerId}")
//    public ResponseEntity logoutUser(@PathVariable String shimmerId){
//        ApplicationUser applicationUser = applicationUserRepository.findByShimmerId(shimmerId);
//        applicationUser.setLoggedIn(false);
//        applicationUserRepository.save(applicationUser);
//        return ResponseEntity.ok(HttpStatus.OK);
//    }

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

    public List<Resource> generateObservationList(String shimKey, String jsonResponse) throws IOException{
        //parse JSON response. It is of format
        //sample response
        //{
        //    "shim": "googlefit",
        //    "timeStamp": 1534251049,
        //    "body": [
        //    {
        //        "header": {
        //            "id": "3b9b68a2-e0fd-4bdd-ba85-4127a4e8bcee",
        //            "creation_date_time": "2018-08-14T12:50:49.383Z",
        //            "acquisition_provenance": {
        //                "source_name": "some application",
        //                "source_origin_id": "some device"
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
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        List<Resource> observationList = new ArrayList<Resource>();
        Observation observation;

        logger.debug("Looking through JSON nodes");
        long timestamp = jsonNode.get("timeStamp").asLong();
        logger.debug("timestamp: " + timestamp);

        for( JsonNode omhStepCount : jsonNode.get("body")){
            logger.debug("Looking at json node");
            logger.debug(omhStepCount.toString());
            observation = generateObservation(shimKey, timestamp, omhStepCount);
            observationList.add(observation);
        }
        return observationList;
    }

    public Observation generateObservation(String shimKey, long timestamp, JsonNode omhStepCount){
        logger.debug("Generating Observation");
        JsonNode omhStepCountBody = omhStepCount.get("body");
        logger.debug("got body");

        JsonNode omhStepCountHeader = omhStepCount.get("header");
        logger.debug("got header");

        String identifier = omhStepCountHeader.get("id").asText();
        logger.debug("identifier: " + identifier);

        String startDateStr = omhStepCountBody.get("effective_time_frame").get("time_interval").get("start_date_time").asText();
        logger.debug("startDateStr: [" + startDateStr + "]");

        String endDateStr = omhStepCountBody.get("effective_time_frame").get("time_interval").get("end_date_time").asText();
        logger.debug("endDateStr: [" + endDateStr + "]");

        int stepCount = omhStepCountBody.get("step_count").asInt();
        logger.debug("stepCount: " + stepCount);

        String deviceSource = omhStepCountHeader.get("acquisition_provenance").get("source_name").asText();
        logger.debug("deviceSource: " + deviceSource);

        String deviceOrigin = omhStepCountHeader.get("acquisition_provenance").get("source_origin_id").asText();
        logger.debug("deviceOrigin: " + deviceOrigin);

        List<String> deviceInfoList = new ArrayList<String>();
        deviceInfoList.add(deviceSource);
        deviceInfoList.add(deviceOrigin);
        deviceInfoList.add(Long.toString(timestamp));
        String deviceInfo = String.join(",", deviceInfoList);

        Observation observation = new Observation();
        //set Id
        observation.setId(UUID.randomUUID().toString());

        //set patient
        Patient patient = generatePatient(shimKey);
        List<Resource> containedResources = new ArrayList<Resource>();
        containedResources.add(patient);
        observation.setContained(containedResources);

        //set identifier
        List<Identifier> idList = createSingleIdentifier(identifier);
        observation.setIdentifier(idList);

        //set status
        observation.setStatus(Observation.ObservationStatus.UNKNOWN);

        //set category
        List<CodeableConcept> categories = createCategory();
        observation.setCategory(categories);

        //set code
        CodeableConcept code = createCodeableConcept(OBSERVATION_CODE_SYSTEM, OBSERVATION_CODE_CODE, OBSERVATION_CODE_DISPLAY);
        observation.setCode(code);

        //set subject
        Reference subjectRef = createSubjectReference(patient.getId());
        observation.setSubject(subjectRef);

        //set effective period
        try {
            Period effectivePeriod = createEffectiveDateTime(startDateStr, endDateStr);
            observation.setEffective(effectivePeriod);
        }
        catch(ParseException pe){
            logger.warn("Could not parse Shimmer dates");
            pe.printStackTrace();
        }

        //set Issued
        observation.setIssued(new Date(timestamp));

        //set device
        Reference deviceRef = createDeviceReference(deviceInfo);
        observation.setDevice(deviceRef);

        //set steps
        List<Observation.ObservationComponentComponent> componentList = new ArrayList<Observation.ObservationComponentComponent>();
        Observation.ObservationComponentComponent occ = createObservationComponent(stepCount);
        componentList.add(occ);
        observation.setComponent(componentList);
        return observation;
    }

    public Patient generatePatient(String shimKey){
        Patient patient = new Patient();
        //set id
        patient.setId(PATIENT_RESOURCE_ID);
        //set identifier
        List<Identifier> idList = createSingleIdentifier(shimKey);
        patient.setIdentifier(idList);
        return patient;
    }

    public Reference createSubjectReference(String refId){
        return createReference(refId, null);
    }
    public Reference createDeviceReference(String display){
        return createReference(null, display);
    }

    public Reference createReference(String refId, String display){
        Reference reference = new Reference();
        if( refId != null ){
            reference.setReference(refId);
        }
        if(display != null){
            reference.setDisplay(display);
        }
        return reference;
    }

    public Identifier createIdentifier(String id){
        Identifier identifier = new Identifier();
        identifier.setSystem(PATIENT_IDENTIFIER_SYSTEM);
        identifier.setValue(id);
        return identifier;
    }

    public List<Identifier> createSingleIdentifier(String id){
        Identifier identifier = createIdentifier(id);
        List<Identifier> idList = new ArrayList<Identifier>();
        idList.add(identifier);
        return idList;
    }

    public List<CodeableConcept> createCategory(){
        CodeableConcept codeableConcept = createCodeableConcept(OBSERVATION_CATEGORY_SYSTEM, OBSERVATION_CATEGORY_CODE, OBSERVATION_CATEGORY_DISPLAY);
        List<CodeableConcept> codeableConceptList = new ArrayList<>();
        codeableConceptList.add(codeableConcept);
        return codeableConceptList;
    }

    public CodeableConcept createCodeableConcept(String system, String code, String display){
        return createCodeableConcept(system, code, display, null);
    }
    public CodeableConcept createCodeableConcept(String system, String code, String display, String text){
        CodeableConcept codeableConcept = new CodeableConcept();
        List<Coding> codingList = new ArrayList<>();
        Coding coding = new Coding();
        coding.setSystem(system);
        coding.setCode(code);
        coding.setDisplay(display);
        codingList.add(coding);
        codeableConcept.setCoding(codingList);
        if(text!= null){
            codeableConcept.setText(text);
        }
        return  codeableConcept;
    }

    public Period createEffectiveDateTime(String startDateStr, String endDateStr) throws ParseException{
        Period effectivePeriod = new Period();
        Date startDate = sdf.parse(startDateStr);
        Date endDate = sdf.parse(endDateStr);
        effectivePeriod.setStart(startDate);
        effectivePeriod.setEnd(endDate);
        return effectivePeriod;
    }

    public Observation.ObservationComponentComponent createObservationComponent(int stepCount){
        Observation.ObservationComponentComponent occ = new Observation.ObservationComponentComponent();
        //set code
        CodeableConcept codeableConcept = createCodeableConcept(OBSERVATION_COMPONENT_CODE_SYSTEM, OBSERVATION_COMPONENT_CODE_CODE, OBSERVATION_COMPONENT_CODE_DISPLAY, OBSERVATION_COMPONENT_CODE_TEXT);
        occ.setCode(codeableConcept);

        //set valueQuantity
        Quantity quantity = new Quantity();
        quantity.setValue(stepCount);
        quantity.setUnit(OBSERVATION_COMPONENT_VALUE_CODE_UNIT);
        quantity.setSystem(OBSERVATION_COMPONENT_VALUE_CODE_SYSTEM);
        quantity.setCode(OBSERVATION_COMPONENT_VALUE_CODE_CODE);
        occ.setValue(quantity);

        return occ;
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
        Bundle bundle = makeBundleWithSingleEntry(binary);

        //return the bundle
        return bundle;
    }

    private Binary makeBinary(byte[] base64EncodedData){
        Binary binary = new Binary();
        binary.setContentType("application/json");
        binary.setContent(base64EncodedData);
        return binary;
    }

    private Bundle makeBundleWithSingleEntry(Resource fhirResource){
        List<Resource> fhirResources = new ArrayList<Resource>();
        fhirResources.add(fhirResource);
        return makeBundle(fhirResources);
    }
    private Bundle makeBundle(List<Resource> fhirResources){
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(fhirResources.size());

        for( Resource fhirResource : fhirResources){
            Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent();
            bundleEntryComponent.setResource(fhirResource);
            bundle.addEntry(bundleEntryComponent);
        }

        return bundle;
    }

    private String getShimmerId(String ehrId, String shimkey){
        logger.debug("Checking User");
        ApplicationUserId applicationUserId = new ApplicationUserId(ehrId, shimkey);
        ApplicationUser user = applicationUserRepository.findById(applicationUserId).orElse(createNewApplicationUser(applicationUserId));
        String shimmerId = user.getShimmerId();
        logger.debug("Returning shimmer id: " + shimmerId);
        return shimmerId;
    }

    private ApplicationUser createNewApplicationUser(ApplicationUserId applicationUserId){
        logger.debug("User does not exist, creating");
        String shimmerId = UUID.randomUUID().toString();
        ApplicationUser newUser = new ApplicationUser(applicationUserId, shimmerId);
        applicationUserRepository.save(newUser);
        logger.debug("finished creating user");
        return newUser;
    }
}
