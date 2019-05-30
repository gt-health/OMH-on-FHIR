package org.gtri.hdap.mdata.controller;

import org.gtri.hdap.mdata.jpa.entity.FhirTemplate;
import org.gtri.hdap.mdata.jpa.repository.FhirTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fhirTemplate")
public class FhirTemplateController {
    @Autowired
    private FhirTemplateRepository ftRepo;

    @GetMapping("/{templateId}")
    public ResponseEntity<FhirTemplate> getFhirTemplate(@PathVariable("templateId") String templateId) {
       FhirTemplate ft = ftRepo.findOneByTemplateId(templateId);
       if (ft != null) {
           return ResponseEntity.notFound().build();
       }
       return ResponseEntity.ok().body(ft);
    }
}
