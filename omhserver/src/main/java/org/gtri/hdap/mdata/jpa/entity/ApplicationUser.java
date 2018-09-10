package org.gtri.hdap.mdata.jpa.entity;

import javax.persistence.*;
import java.util.Collection;

/**
 * Created by es130 on 7/6/2018.
 */
@Entity
public class ApplicationUser {
    @EmbeddedId
    private ApplicationUserId applicationUserId;
    private String shimmerId;
    private Boolean loggedIn;

    /*========================================================================*/
    /* Constructors */
    /*========================================================================*/
    protected ApplicationUser(){}

    public ApplicationUser(ApplicationUserId applicationUserId, String shimmerId){
        this(applicationUserId, shimmerId, false);
    }

    public ApplicationUser(ApplicationUserId applicationUserId, String shimmerId, Boolean loggedIn){
        this.applicationUserId = applicationUserId;
        this.shimmerId = shimmerId;
        this.loggedIn = loggedIn;
    }
    /*========================================================================*/
    /* Getters */
    /*========================================================================*/

    public ApplicationUserId getApplicationUserId() {
        return applicationUserId;
    }

    public String getShimmerId() {
        return shimmerId;
    }

    public Boolean getLoggedIn() {
        return loggedIn;
    }
    /*========================================================================*/
    /* Setters */
    /*========================================================================*/

    public void setApplicationUserId(ApplicationUserId applicationUserId) {
        this.applicationUserId = applicationUserId;
    }

    public void setShimmerId(String shimmerId) {
        this.shimmerId = shimmerId;
    }

    public void setLoggedIn(Boolean loggedIn) {
        this.loggedIn = loggedIn;
    }
    /*========================================================================*/
    /* Methods */
    /*========================================================================*/

    @Override
    public String toString(){
        return String.format("ApplicationUser ehrId: '%s', shimmerId: '%s', shimKey: '%s', is logged in: '%s'", applicationUserId.getEhrId(), shimmerId, getApplicationUserId().getShimKey(), loggedIn);
    }
}
