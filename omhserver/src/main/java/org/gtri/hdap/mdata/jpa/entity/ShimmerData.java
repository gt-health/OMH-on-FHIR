package org.gtri.hdap.mdata.jpa.entity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.UUID;

/**
 * Created by es130 on 7/9/2018.
 */
@Entity
public class ShimmerData {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    private String documentId;
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String jsonData;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "ehr_id", referencedColumnName = "ehr_id", nullable = false),
        @JoinColumn(name = "shim_key", referencedColumnName = "shim_key", nullable = false)
    })
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ApplicationUser applicationUser;

    /*========================================================================*/
    /* Constructors */
    /*========================================================================*/
    protected ShimmerData(){}

    public ShimmerData(ApplicationUser applicationUser, String jsonData){
        this(applicationUser, UUID.randomUUID().toString(), jsonData);
    }

    public ShimmerData(ApplicationUser applicationUser, String documentId, String jsonData){
        this.applicationUser = applicationUser;
        this.jsonData = jsonData;
        this.documentId = documentId;
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

    public String getDocumentId() {
        return documentId;
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

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /*========================================================================*/
    /* Methods */
    /*========================================================================*/
    @Override
    public String toString(){
        return String.format("ShimmerData[id='%d', docId='%s', json=%s", id, documentId, jsonData);
    }
}
