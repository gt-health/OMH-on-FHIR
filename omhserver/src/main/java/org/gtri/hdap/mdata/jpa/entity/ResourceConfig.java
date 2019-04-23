package org.gtri.hdap.mdata.jpa.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.annotations.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.IOException;
import java.util.List;

@Entity
public class ResourceConfig {

    @Id
    @Column(name="resourceId")
    private String resourceId;

//    @Type(type="jsonb")
    @Column(name="config")
    private String config;

    private final Logger logger = LoggerFactory.getLogger(ResourceConfig.class);
    private ObjectMapper mapper = new ObjectMapper();

    public ResourceConfig() {
    }

    public ResourceConfig(String resourceId, JsonNode config) {
        this.resourceId = resourceId;
        this.config = config.toString();
    }

    public String getResourceName() {
        return resourceId;
    }

    public void setResourceName(String resourceId) {
        this.resourceId = resourceId;
    }

    public JsonNode getConfig() {
        try {
            return mapper.readTree(this.config);
        } catch (IOException e) {
            System.err.println("Failed to parse json when retrieving resource config for resource :" + this.resourceId);
            System.err.println(this.config);
            e.printStackTrace();
            return null;
        }
    }

    public void setConfig(JsonNode config) {
        this.config = config.toString();
    }

}
