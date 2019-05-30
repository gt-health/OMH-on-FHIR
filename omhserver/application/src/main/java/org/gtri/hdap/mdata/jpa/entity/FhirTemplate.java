package org.gtri.hdap.mdata.jpa.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.IOException;

@Entity
@Table(name="fhir_template")
public class FhirTemplate {
    @Id
    @Column
    private String templateId;

    @Column(columnDefinition = "TEXT")
    private String template;

    public FhirTemplate() {}

    public FhirTemplate(String templateId, JsonNode template) {
        this.templateId = templateId;
        this.template = template.toString();
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public JsonNode getTemplate() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonTemplate = mapper.readTree(template);
            return jsonTemplate;
        } catch (IOException e) {
            return null;
        }
    }

    public void setTemplate(JsonNode template) {
        this.template = template.toString();
    }
}
