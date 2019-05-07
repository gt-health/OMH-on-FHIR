package org.gtri.hdap.mdata.common.jpa.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.IOException;

@Entity
@Table(name="resource_config")
public class ResourceConfig {

    @Id
    @Column(name="resourceId")
    private String resourceId;

    @Column(name="config", columnDefinition="TEXT")
    private String config;

    /*========================================================================*/
    /* Constructors */
    /*========================================================================*/

    public ResourceConfig() {
    }

    public ResourceConfig(String resourceId, JsonNode config) {
        this.resourceId = resourceId;
        this.config = config.toString();
    }

    /*========================================================================*/
    /* Getters */
    /*========================================================================*/

    public String getResourceId() {
        return resourceId;
    }

    public JsonNode getConfig() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readTree(this.config);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /*========================================================================*/
    /* Setters */
    /*========================================================================*/

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public void setConfig(JsonNode config) {
        this.config = config.toString();
    }

}
