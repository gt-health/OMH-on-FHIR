package org.gtri.hdap.mdata.jpa.entity;

import javax.persistence.*;

/**
 * Created by es130 on 7/6/2018.
 */
@Entity
public class ApplicationUser {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    @Column(unique=true)
    private String ehrId;
    private String shimmerId;
    private String shimKey;

    /*========================================================================*/
    /* Constructors */
    /*========================================================================*/
    protected ApplicationUser(){}

    public ApplicationUser(String ehrId, String shimmerId, String shimKey){
        this.ehrId = ehrId;
        this.shimmerId = shimmerId;
        this.shimKey = shimKey;
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

    public String getShimKey() {
        return shimKey;
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

    public void setShimKey(String shimKey) {
        this.shimKey = shimKey;
    }
/*========================================================================*/
    /* Methods */
    /*========================================================================*/

    @Override
    public String toString(){
        return String.format("ApplicationUser id:'%d', ehrId: '%s', shimmerId: '%s', shimKey: '%s'", id, ehrId, shimmerId, shimKey);
    }
}
