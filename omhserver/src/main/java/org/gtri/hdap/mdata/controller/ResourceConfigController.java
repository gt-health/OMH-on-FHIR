package org.gtri.hdap.mdata.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import jdk.nashorn.internal.ir.ObjectNode;
import org.gtri.hdap.mdata.jpa.entity.ResourceConfig;
import org.gtri.hdap.mdata.jpa.repository.ResourceConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config")
@Api(value="resourceconfigcontroller", description="OMH on FHIR configuration controller.")
public class ResourceConfigController {
    @Autowired
    private ResourceConfigRepository rcRepo;

    private final ObjectMapper mapper = new ObjectMapper();

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
