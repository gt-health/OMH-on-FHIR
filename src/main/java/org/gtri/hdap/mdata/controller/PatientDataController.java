package org.gtri.hdap.mdata.controller;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.gtri.hdap.mdata.jpa.entity.ApplicationUser;
import org.gtri.hdap.mdata.jpa.entity.ShimmerData;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

/**
 * Created by es130 on 6/27/2018.
 */
@RestController
public class PatientDataController {

    /*========================================================================*/
    /* Constants */
    /*========================================================================*/
    public static String SHIMMER_SERVER_URL_BASE_ENV = "SHIMMER_SERVER_URL";
    public static String SHIMMER_AUTH_URL = "/authorize/{shimKey}?username={username}";
    public static String SHIMMER_DATA_RANGE_URL = "/data/{shim-key}/physical_activity?username={username}&normalize={normalize}&dateStart={start-date}&dateEnd={end-date}";

    /*========================================================================*/
    /* Variables */
    /*========================================================================*/
    private final Logger logger = LoggerFactory.getLogger(PatientDataController.class);
    private CloseableHttpClient httpClient = null;
    private HttpClientContext httpClientContext = null;

    /*========================================================================*/
    /* Getters */
    /*========================================================================*/

    public CloseableHttpClient getHttpClient(){
        if( httpClient == null ){
            httpClient = createHttpClient();
        }
        return httpClient;
    }

    public HttpClientContext getHttpClientContext() {
        if( httpClientContext == null ){
            httpClientContext = HttpClientContext.create();
        }
        return httpClientContext;
    }

    /*========================================================================*/
    /* Setters */
    /*========================================================================*/

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setHttpClientContext(HttpClientContext httpClientContext) {
        this.httpClientContext = httpClientContext;
    }

    /*========================================================================*/
    /* Public Methods */
    /*========================================================================*/
    @RequestMapping("/")
    public String index(){
        return "Greetings from Spring Boot!";
    }

//    @RequestMapping(value="/shimmerAuthentication", method= RequestMethod.GET)
    @GetMapping("/shimmerAuthentication")
    public ModelAndView authenticateWithShimmer(ModelMap model){
        logger.debug("Trying to connect to FitBit API");
        // Make a request to http://<shimmer-host>:8083/authorize/{shimKey}?username={userId}
        // The shimKey path parameter should be one of the keys listed below, e.g. fitbit
        // for example https://gt-apps.hdap.gatech.edu:8083/authorize/fitbit?username={userId}
        // The username query parameter can be set to any unique identifier you'd like to use to identify the user.

        String shimmerAuthUrl = System.getenv(SHIMMER_SERVER_URL_BASE_ENV) + SHIMMER_AUTH_URL;
        shimmerAuthUrl = shimmerAuthUrl.replace("{shimKey}", "fitbit");
        shimmerAuthUrl = shimmerAuthUrl.replace("{username}", "3f6625db-8cc7-4d25-9bf4-9febdc7028cd");
        HttpGet httpGet = new HttpGet(shimmerAuthUrl);
        String fitbitAuthUrl = processShimmerAuthRequest(httpGet);
        if( fitbitAuthUrl == null ){
            //return a 500 error
            logger.debug("Did not get an authorization URL");
        }

        logger.debug("Finished connection to FitBit API");
//        return "This page handles shimmer user auth";
        String redirectUrl = "redirect:" + fitbitAuthUrl;
        return new ModelAndView(redirectUrl, model);
    }

    @GetMapping("/activity")
    public ResponseEntity<?> retrievePatientData(){
        String shimmerAuthUrl = System.getenv(SHIMMER_SERVER_URL_BASE_ENV) + SHIMMER_AUTH_URL;
        shimmerAuthUrl = shimmerAuthUrl.replace("{shimKey}", "fitbit");
        shimmerAuthUrl = shimmerAuthUrl.replace("{username}", "3f6625db-8cc7-4d25-9bf4-9febdc7028cd");
        shimmerAuthUrl = shimmerAuthUrl.replace("{normalize}", "true");
        shimmerAuthUrl = shimmerAuthUrl.replace("{start-date}", "2018-06-01");
        shimmerAuthUrl = shimmerAuthUrl.replace("{end-date}", "2018-07-17");
        HttpGet httpGet = new HttpGet(shimmerAuthUrl);
        String jsonResponse = processShimmerDataRequest(httpGet);
        if(jsonResponse == null){
            //return a 500 error
            logger.debug("Did not find data");
        }

        //TODO replace this code with queries to JPA
        ApplicationUser appUser = new ApplicationUser("ehr1234", "3f6625db-8cc7-4d25-9bf4-9febdc7028cd");
        ShimmerData shimmerData = new ShimmerData(appUser, jsonResponse);
        return ResponseEntity.ok(shimmerData);
    }

    @RequestMapping("/authorize/fitbit/callback")
    public String handleFitbitRedirect(){
        logger.debug("Handling successful Fitbit auth redirect");
        return "TODO handle successful Fitbit auth redirect";
    }

    /*========================================================================*/
    /* Private Methods */
    /*========================================================================*/

    /**
     * Creates a {@link CloseableHttpClient} to use in the application
     * @return
     */
    private CloseableHttpClient createHttpClient(){
        //TODO: Fix to use non-deprecated code
        RequestConfig globalConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .build();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(globalConfig)
//                .setSSLSocketFactory(getSslsf())
//                .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                .build();
        return httpClient;
    }

    private String processShimmerAuthRequest(HttpUriRequest request){
        String authorizationUrl = null;
        try {
            CloseableHttpResponse shimmerAuthResponse = getHttpClient().execute(request, getHttpClientContext());
            authorizationUrl = checkShimmerAuthResponse(shimmerAuthResponse);
        }
        catch(IOException ioe){
            ioe.printStackTrace();
        }
        return authorizationUrl;
    }

    private String checkShimmerAuthResponse(CloseableHttpResponse shimmerAuthResponse) throws IOException {
        String authorizationUrl = null;
        try {
            int statusCode = shimmerAuthResponse.getStatusLine().getStatusCode();
            logger.debug("Response Code " + statusCode);
            if(statusCode == 200) {
                //All we need is the cookie, which is managed as part of the HTTPContext
                //for now ignore the content of the response. At a later date process
                //the JSON. It contains permission and user metadata.
                HttpEntity responseEntity = shimmerAuthResponse.getEntity();
                //get the json from the response and get the auth URL to redirect the user
                String responseStr = EntityUtils.toString(responseEntity);
                JSONObject responseJson = new JSONObject(responseStr);
                authorizationUrl = responseJson.getString("authorizationUrl");
                logger.debug("Authorization URL " + authorizationUrl);
            }
        } finally {
            shimmerAuthResponse.close();
        }
        return authorizationUrl;
    }

    private String processShimmerDataRequest(HttpUriRequest request){
        String jsonResp = null;
        try {
            CloseableHttpResponse shimmerAuthResponse = getHttpClient().execute(request, getHttpClientContext());
            jsonResp = checkShimmerAuthResponse(shimmerAuthResponse);
        }
        catch(IOException ioe){
            ioe.printStackTrace();
        }
        return jsonResp;
    }

    private String checkShimmerDataResponse(CloseableHttpResponse shimmerAuthResponse) throws IOException {
        String jsonResp = null;
        try {
            int statusCode = shimmerAuthResponse.getStatusLine().getStatusCode();
            logger.debug("Response Code " + statusCode);
            if(statusCode == 200) {
                //All we need is the cookie, which is managed as part of the HTTPContext
                //for now ignore the content of the response. At a later date process
                //the JSON. It contains permission and user metadata.
                HttpEntity responseEntity = shimmerAuthResponse.getEntity();
                //get the json from the response and get the auth URL to redirect the user
                jsonResp = EntityUtils.toString(responseEntity);
                logger.debug("Data Response: " + jsonResp);
            }
        } finally {
            shimmerAuthResponse.close();
        }
        return jsonResp;
    }
}
