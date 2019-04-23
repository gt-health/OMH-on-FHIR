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

    @Column(name="config")
    private String config;

    private ObjectMapper mapper = new ObjectMapper();

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

    public String getResourceName() {
        return resourceId;
    }

    public JsonNode getConfig() {
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

    public void setResourceName(String resourceId) {
        this.resourceId = resourceId;
    }

    public void setConfig(JsonNode config) {
        this.config = config.toString();
    }

}
