package org.gtri.hdap.mdata.service;

/**
 * Created by es130 on 9/12/2018.
 */
public class ShimmerResponse {

    /*========================================================================*/
    /* Variables */
    /*========================================================================*/
    public int responseCode;
    public String responseData;

    /*========================================================================*/
    /* Constructors */
    /*========================================================================*/

    public ShimmerResponse(int responseCode, String responseData) {
        this.responseCode = responseCode;
        this.responseData = responseData;
    }

    /*========================================================================*/
    /* GETTERS */
    /*========================================================================*/
    public int getResponseCode() {
        return responseCode;
    }
    public String getResponseData() {
        return responseData;
    }

    /*========================================================================*/
    /* SETTERS */
    /*========================================================================*/
    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }
    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }
}
