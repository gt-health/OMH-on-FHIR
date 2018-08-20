package org.gtri.hdap.mdata.jpa.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by es130 on 8/20/2018.
 */
@Embeddable
public class ApplicationUserId implements Serializable{

    /*========================================================================*/
    /* Variables */
    /*========================================================================*/
    @Column(name="ehr_id")
    private String ehrId;
    @Column(name="shim_key")
    private String shimKey;

    /*========================================================================*/
    /* Constructors */
    /*========================================================================*/
    protected ApplicationUserId(){}
    public ApplicationUserId(String ehrId, String shimKey) {
        this.ehrId = ehrId;
        this.shimKey = shimKey;
    }

    /*========================================================================*/
    /* Getters */
    /*========================================================================*/
    public String getEhrId() {
        return ehrId;
    }

    public String getShimKey() {
        return shimKey;
    }

    /*========================================================================*/
    /* Setters */
    /*========================================================================*/
    public void setEhrId(String ehrId) {
        this.ehrId = ehrId;
    }

    public void setShimKey(String shimKey) {
        this.shimKey = shimKey;
    }
}
