package org.gtri.hdap.mdata.jpa.entity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

/**
 * Created by es130 on 7/9/2018.
 */
@Entity
public class ShimmerData {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    private String jsonData;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ApplicationUser applicationUser;

    /*========================================================================*/
    /* Constructors */
    /*========================================================================*/
    protected ShimmerData(){}

    public ShimmerData(ApplicationUser applicationUser, String jsonData){
        this.applicationUser = applicationUser;
        this.jsonData = jsonData;
    }

    /*========================================================================*/
    /* Getters */
    /*========================================================================*/

    public Long getId() {
        return id;
    }

    public ApplicationUser getApplicationUser() {
        return applicationUser;
    }

    public String getJsonData() {
        return jsonData;
    }

    /*========================================================================*/
    /* Setters */
    /*========================================================================*/

    public void setId(Long id) {
        this.id = id;
    }

    public void setApplicationUser(ApplicationUser applicationUser) {
        this.applicationUser = applicationUser;
    }

    public void setJsonData(String jsonData) {
        this.jsonData = jsonData;
    }

    /*========================================================================*/
    /* Methods */
    /*========================================================================*/
    @Override
    public String toString(){
        return String.format("ShimmerData[id='%d', json=%s", id, jsonData);
    }
}
