package org.gtri.hdap.mdata.dstu3.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import org.gtri.hdap.mdata.common.jpa.entity.*;
import org.gtri.hdap.mdata.common.jpa.repository.ApplicationUserRepository;
import org.gtri.hdap.mdata.common.jpa.repository.FhirTemplateRepository;
import org.gtri.hdap.mdata.common.jpa.repository.ResourceConfigRepository;
import org.gtri.hdap.mdata.common.jpa.repository.ShimmerDataRepository;
import org.gtri.hdap.mdata.common.util.ShimmerUtil;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by es130 on 9/14/2018.
 */
@Service
public class Dstu3ResponseService {

    /*========================================================================*/
    /* Constants */
    /*========================================================================*/


    /*========================================================================*/
    /* Constructors */
    /*========================================================================*/
    public Dstu3ResponseService() {
    }

    /*========================================================================*/
    /* Variables */
    /*========================================================================*/
    @Autowired
    private ApplicationUserRepository applicationUserRepository;
    @Autowired
    private ShimmerDataRepository shimmerDataRepository;
    @Autowired
    private ResourceConfigRepository resourceConfigRepository;
    @Autowired
    private FhirTemplateRepository fhirTemplateRepository;
    private final Logger logger = LoggerFactory.getLogger(Dstu3ResponseService.class);
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private SimpleDateFormat sdfNoMilli = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /*========================================================================*/
    /* Public Methods */
    /*========================================================================*/
    public DocumentReference generateDocumentReference(String documentId, String shimKey){
        DocumentReference documentReference = null;
        if(!documentId.isEmpty()) {
            String title = "OMH " + shimKey + " data";
            String binaryRef = "Binary/" + documentId;
            Date creationDate = new Date();
            //create a DocumentReference to return
            documentReference = new DocumentReference();
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
        }
        return documentReference;
    }

    public List<Resource> transformFhirTemplate(ResourceConfig config, JsonNode omhResponse) throws IOException, JsonPatchException {
        JsonNode fhirResource;
        ObjectMapper mapper = new ObjectMapper();
        FhirContext ctx = FhirContext.forDstu3();
        IParser jsonParser = ctx.newJsonParser();
        List<Resource> finalList = new ArrayList<>();
        ObjectNode configJson = (ObjectNode)config.getConfig();
        ArrayNode omhPatches = (ArrayNode)configJson.at("/patches/omh");
        ArrayNode otherPatches = (ArrayNode)configJson.at("/patches/other");
        ArrayNode omhDatapoints = (ArrayNode)omhResponse.get("body");
        FhirTemplate fhirTemplate = fhirTemplateRepository.findOneByTemplateId(configJson.at("/fhirTemplate").textValue());
        if (fhirTemplate == null) {
            throw new IOException("Fhir template specified in resource config not found. Please check your config.");
        }
        for (int i = 0; i < omhDatapoints.size(); i++) {
            fhirResource = fhirTemplate.getTemplate();
            ObjectNode omhData = (ObjectNode)omhDatapoints.get(i);
            for (int j = 0; j < omhPatches.size(); j++) {
                ObjectNode omhPatch = (ObjectNode)omhPatches.get(j);
                String op = omhPatch.get("op").textValue();
                String fhirPath = omhPatch.get("fhirPath").textValue();
                String omhPath = omhPatch.get("omhPath").textValue();
                if (!omhData.at(omhPath).isMissingNode()) {
                    ArrayNode patchArray = mapper.createArrayNode();
                    ObjectNode patchObj = mapper.createObjectNode();
                    patchObj.put("op", op);
                    patchObj.put("path", fhirPath);
                    patchObj.set("value", omhData.at(omhPath));
                    patchArray.add(patchObj);
                    try {
                        JsonPatch patch = JsonPatch.fromJson(patchArray);
                        fhirResource = patch.apply(fhirResource);
                    } catch (JsonPatchException e) {
                        logger.error("Patch error on FHIR resource for patch: " + patchObj.toString());
                        e.printStackTrace();
                        throw e;
                    }
                } else if(!omhPatch.get("required").booleanValue()) {
                    logger.warn("OMH data at " + omhPath + " not found and marked as not required. ignoring.");
                    continue;
                } else {
                    logger.error("OMH data at " + omhPath + " not found and marked as required.");
                    throw new IOException("OMH data at " + omhPath + " not found and marked as required.");
                }
            }
            JsonPatch patch = JsonPatch.fromJson(otherPatches);
            try {
                fhirResource = patch.apply(fhirResource);
            } catch (JsonPatchException e) {
                logger.error("Patch error on FHIR resource for plain patch.");
                e.printStackTrace();
                return null;
            }
            try {
                finalList.add(jsonParser.parseResource(Observation.class, fhirResource.toString()));
            } catch (DataFormatException e) {
                logger.error("Transformed FHIR template could not be mapped to a valid FHIR resource.");
                logger.error("Please check the config mappings to ensure all requirements are met for FHIR Observation data spec.");
                e.printStackTrace();
                throw e;
            }
        }
        return finalList;
    }

    public List<Resource> generateObservations(String omhResponse, ResourceConfig resourceConfig) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode omhJsonNode = objectMapper.readTree(omhResponse);
        String resourceId = resourceConfig.getResourceId();
        long timestamp = omhJsonNode.get("timeStamp").asLong();
        List<Resource> observationList = new ArrayList<>();
        try {
            observationList = transformFhirTemplate(resourceConfig, omhJsonNode);
        } catch (JsonPatchException e) {
            logger.error("FHIR transformation failed at timestamp: " + timestamp);
            return null;
        }
        return observationList;
    }

    public Patient generatePatient(String shimKey){
        Patient patient = new Patient();
        //set id
        patient.setId(ShimmerUtil.PATIENT_RESOURCE_ID);
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
        identifier.setSystem(ShimmerUtil.PATIENT_IDENTIFIER_SYSTEM);
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
        CodeableConcept codeableConcept = createCodeableConcept(ShimmerUtil.OBSERVATION_CATEGORY_SYSTEM, ShimmerUtil.OBSERVATION_CATEGORY_CODE, ShimmerUtil.OBSERVATION_CATEGORY_DISPLAY);
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
        Date startDate = parseDate(startDateStr);
        Date endDate = parseDate(endDateStr);
        effectivePeriod.setStart(startDate);
        effectivePeriod.setEnd(endDate);
        return effectivePeriod;
    }

    public Date parseDate(String dateStr) throws ParseException{
        Date date;
        try{
            date =sdf.parse(dateStr);
        }
        catch(ParseException pe){
            date = sdfNoMilli.parse(dateStr);
        }
        return date;
    }

    public Observation.ObservationComponentComponent createObservationComponent(int stepCount){
        Observation.ObservationComponentComponent occ = new Observation.ObservationComponentComponent();
        //set code
        CodeableConcept codeableConcept = createCodeableConcept(ShimmerUtil.OBSERVATION_COMPONENT_CODE_SYSTEM, ShimmerUtil.OBSERVATION_COMPONENT_CODE_CODE, ShimmerUtil.OBSERVATION_COMPONENT_CODE_DISPLAY, ShimmerUtil.OBSERVATION_COMPONENT_CODE_TEXT);
        occ.setCode(codeableConcept);

        //set valueQuantity
        Quantity quantity = new Quantity();
        quantity.setValue(stepCount);
        quantity.setUnit(ShimmerUtil.OBSERVATION_COMPONENT_VALUE_CODE_UNIT);
        quantity.setSystem(ShimmerUtil.OBSERVATION_COMPONENT_VALUE_CODE_SYSTEM);
        quantity.setCode(ShimmerUtil.OBSERVATION_COMPONENT_VALUE_CODE_CODE);
        occ.setValue(quantity);

        return occ;
    }

    public byte[] makeByteArrayForDocument(String documentId){
        logger.debug("Making Bundle for Document" + documentId);
        //get fitbit data for binary resource
        ShimmerData shimmerData = shimmerDataRepository.findByDocumentId(documentId);
        //get shimmer data
        String jsonData = "";
        if(shimmerData != null) {
            jsonData = shimmerData.getJsonData();
            logger.debug("Got JSON data " + jsonData);
            //Delete the stored user data because it has been retrieved. We do not want to hold on to data longer than needed.
            shimmerDataRepository.delete(shimmerData);
        }
        return jsonData.getBytes();
    }

    public Bundle makeBundleForDocument(String documentId){

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

    public Binary makeBinary(byte[] base64EncodedData){
        Binary binary = new Binary();
        binary.setContentType("application/json");
        binary.setContent(base64EncodedData);
        return binary;
    }

    public Bundle makeBundleWithSingleEntry(Resource fhirResource){
        List<Resource> fhirResources = new ArrayList<Resource>();
        if( fhirResource != null ) {
            fhirResources.add(fhirResource);
        }
        return makeBundle(fhirResources);
    }
    public Bundle makeBundle(List<Resource> fhirResources){
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

    public String getShimmerId(String ehrId, String shimkey){
        logger.debug("Checking User EHR ID: [" + ehrId + "] ShimKey: [" + shimkey + "]");
        ApplicationUserId applicationUserId = new ApplicationUserId(ehrId, shimkey);
        //debug info
        logger.debug("Find by ID " + applicationUserRepository.findById(applicationUserId).isPresent());
        ApplicationUser user;
        Optional<ApplicationUser> applicationUserOptional = applicationUserRepository.findById(applicationUserId);
        if(applicationUserOptional.isPresent()){
            logger.debug("Found the user");
            user = applicationUserOptional.get();
        }
        else{
            logger.debug("Did not find user. Creating new one");
            user = createNewApplicationUser(applicationUserId);
        }
        String shimmerId = user.getShimmerId();
        logger.debug("Returning shimmer id: " + shimmerId);
        return shimmerId;
    }

    public ApplicationUser createNewApplicationUser(ApplicationUserId applicationUserId){
        logger.debug("User does not exist, creating");
        String shimmerId = UUID.randomUUID().toString();
        ApplicationUser newUser = new ApplicationUser(applicationUserId, shimmerId);
        applicationUserRepository.save(newUser);
        //force a flush
        applicationUserRepository.flush();
        logger.debug("finished creating user");
        return newUser;
    }


    /**
     * Returns the text contained in a JsonNode object or an empty string if the Node is null
     * @param jsonNode
     * @return
     */
    private String getJsonNodeText(JsonNode jsonNode){
        String nodeText = "";
        if(jsonNode != null){
            nodeText = jsonNode.asText();
        }
        return nodeText;
    }
}
