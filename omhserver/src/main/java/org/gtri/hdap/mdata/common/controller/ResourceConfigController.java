package org.gtri.hdap.mdata.common.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import org.gtri.hdap.mdata.common.jpa.entity.ResourceConfig;
import org.gtri.hdap.mdata.common.jpa.repository.ResourceConfigRepository;
import org.gtri.hdap.mdata.dstu3.service.Dstu3ResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config")
@Api(value = "resourceconfigcontroller", description = "OMH on FHIR configuration controller.")
public class ResourceConfigController {
    @Autowired
    private ResourceConfigRepository rcRepo;
    @Autowired
    private Dstu3ResponseService dstu3ResponseService;

    private final ObjectMapper mapper = new ObjectMapper();

    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

//    @GetMapping("/testResourceConfigStuff")
//    public ResponseEntity<List<Observation>> testThisStuff() {
//        try {
////            String tmp = FileUtils.readFileToString(
////                new File(
////                    getClass().getClassLoader().getResource("testconfig1.json").getFile()
////                ),
////                StandardCharsets.UTF_8
////            );
//            String tmp = FileUtils.readFileToString(
//                new File("/testconfig1.json"),
//                StandardCharsets.UTF_8
//            );
//            JsonNode testConfig = mapper.readTree(tmp).get("config");
//            tmp = FileUtils.readFileToString(
//                new File("/testomhresponse1.json"),
//                StandardCharsets.UTF_8
//            );
//            ObjectNode testOmhResponse = (ObjectNode)mapper.readTree(tmp);
//            List<Observation> fhirObjects = dstu3ResponseService.transformFhirTemplate(testConfig, testOmhResponse);
//            return ResponseEntity.ok().body(fhirObjects);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500).build();
//        }
//    }

    @GetMapping("/{resourceId}")
    public ResponseEntity<ResourceConfig> getConfiguration(
            @RequestParam("resourceId") String resourceId
    ) {
        ResourceConfig rc = rcRepo.findOneByResourceId(resourceId);
        if (rc == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(rc);
    }

    @PutMapping(path = "/{resourceId}", produces = "application/json")
    public ResponseEntity<String> pushConfiguration(
            @RequestParam("resourceId") String resourceId,
            @RequestBody JsonNode configObj
    ) {
        ResourceConfig rc = new ResourceConfig(resourceId, configObj);
        rcRepo.save(rc);
        String res = "{\"status\" : \"OK\", \"resourceId\" : \"" + resourceId + "\"}";
        return ResponseEntity.ok().body(res);
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<String> deleteConfiguration(
            @RequestParam("resourceId") String resourceId
    ) {
        try {
            rcRepo.deleteById(resourceId);
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.notFound().build();
        }
        String res = "{\"status\" : \"OK\", \"resourceId\" : \"" + resourceId + "\"}";
        return ResponseEntity.ok().body(res);
    }
}
