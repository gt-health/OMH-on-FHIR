package org.gtri.hdap.mdata.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import org.gtri.hdap.mdata.jpa.entity.ResourceConfig;
import org.gtri.hdap.mdata.jpa.repository.ResourceConfigRepository;
import org.gtri.hdap.mdata.service.Stu3ResponseService;
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
    private Stu3ResponseService stu3ResponseService;

    private final ObjectMapper mapper = new ObjectMapper();

    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @GetMapping("/{resourceId}")
    public ResponseEntity<ResourceConfig> getConfiguration(
            @PathVariable("resourceId") String resourceId
    ) {
        ResourceConfig rc = rcRepo.findOneByResourceId(resourceId);
        if (rc == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(rc);
    }

    @PutMapping(path = "/{resourceId}", produces = "application/json")
    public ResponseEntity<String> pushConfiguration(
            @PathVariable("resourceId") String resourceId,
            @RequestBody JsonNode configObj
    ) {
        ResourceConfig rc = new ResourceConfig(resourceId, configObj);
        rcRepo.save(rc);
        String res = "{\"status\" : \"OK\", \"resourceId\" : \"" + resourceId + "\"}";
        return ResponseEntity.ok().body(res);
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<String> deleteConfiguration(
            @PathVariable("resourceId") String resourceId
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
