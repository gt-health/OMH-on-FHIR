package org.gtri.hdap.mdata.jpa.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Created by es130 on 7/6/2018.
 */
@Entity
public class ApplicationUser {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String ehrId;
    private String shimmerId;

    /*========================================================================*/
    /* Constructors */
    /*========================================================================*/
    protected ApplicationUser(){}

    public ApplicationUser(String ehrId, String shimmerId){
        this.ehrId = ehrId;
        this.shimmerId = shimmerId;
    }

    /*========================================================================*/
    /* Getters */
    /*========================================================================*/

    public Long getId() {
        return id;
    }

    public String getEhrId() {
        return ehrId;
    }

    public String getShimmerId() {
        return shimmerId;
    }

    /*========================================================================*/
    /* Setters */
    /*========================================================================*/

    public void setId(Long id) {
        this.id = id;
    }

    public void setEhrId(String ehrId) {
        this.ehrId = ehrId;
    }

    public void setShimmerId(String shimmerId) {
        this.shimmerId = shimmerId;
    }

    /*========================================================================*/
    /* Methods */
    /*========================================================================*/

    @Override
    public String toString(){
        return String.format("ApplicationUser id:'%d', ehrId: '%s', shimmerId: '%s'", id, ehrId, shimmerId);
    }
}
