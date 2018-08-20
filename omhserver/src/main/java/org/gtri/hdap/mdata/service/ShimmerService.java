package org.gtri.hdap.mdata.service;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
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
import org.gtri.hdap.mdata.jpa.repository.ApplicationUserRepository;
import org.gtri.hdap.mdata.jpa.repository.ShimmerDataRepository;
import org.gtri.hdap.mdata.util.ShimmerUtil;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by es130 on 7/19/2018.
 */
@Service
public class ShimmerService {


    /*========================================================================*/
    /* Constants*/
    /*========================================================================*/
    public static String SHIMMER_SERVER_URL_BASE_ENV = "SHIMMER_SERVER_URL";
    public static String SHIMMER_SERVER_REDIRECT_URL_ENV = "SHIMMER_REDIRECT_URL";
    public static String SHIMMER_AUTH_URL = "/authorize/{shim-key}?username={username}&redirect_url={redirect-url}";
    public static String SHIMMER_AUTH_CALLBACK = "/authorize/{shim-key}/callback?code={code}&state={state}";
//    public static String SHIMMER_AUTH_CALLBACK = "/authorize/{shim-key}/callback?state={state}";
//public static String SHIMMER_DATA_RANGE_URL = "/data/{shim-key}/physical_activity?username={username}&normalize={normalize}";
    public static String SHIMMER_DATA_RANGE_URL = "/data/{shim-key}/step_count?username={username}&normalize={normalize}";
    public static String SHIMMER_START_DATE_URL_PARAM = "&dateStart={start-date}";
    public static String SHIMMER_END_DATE_URL_PARAM = "&dateEnd={end-date}";
    public static String START_DATE_KEY = "startDate";
    public static String END_DATE_KEY = "endDate";

    /*========================================================================*/
    /* Constructors */
    /*========================================================================*/
    public ShimmerService(){

    }

    /*========================================================================*/
    /* Variables */
    /*========================================================================*/
    private final Logger logger = LoggerFactory.getLogger(ShimmerService.class);
    private CloseableHttpClient httpClient = null;
    private HttpClientContext httpClientContext = null;

    @Autowired
    private ApplicationUserRepository applicationUserRepository;
    @Autowired
    private ShimmerDataRepository shimmerDataRepository;

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
    public String processShimmerAuthRequest(HttpUriRequest request){
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

    public String requestShimmerAuthUrl(String shimmerId, String shimkey) throws Exception{
        String shimmerAuthUrl = System.getenv(SHIMMER_SERVER_URL_BASE_ENV) + SHIMMER_AUTH_URL;
        String shimmerRedirectUrl = System.getenv(SHIMMER_SERVER_REDIRECT_URL_ENV);
        shimmerAuthUrl = shimmerAuthUrl.replace("{shim-key}", shimkey);
        shimmerAuthUrl = shimmerAuthUrl.replace("{username}", shimmerId);
        shimmerAuthUrl = shimmerAuthUrl.replace("{redirect-url}", shimmerRedirectUrl);
        logger.debug("Sending authorization request to " + shimmerAuthUrl);
        HttpGet httpGet = new HttpGet(shimmerAuthUrl);
        String authUrl = processShimmerAuthRequest(httpGet);
        if( authUrl == null ){
            throw new Exception("Could not retrieve authorization URL for " + shimkey);
        }
        return authUrl;
    }

    public void completeShimmerAuth(String shimkey, String code, String state) throws Exception{
        String shimmerAuthCallbackUrl = System.getenv(SHIMMER_SERVER_URL_BASE_ENV) + SHIMMER_AUTH_CALLBACK;
        shimmerAuthCallbackUrl = shimmerAuthCallbackUrl.replace("{shim-key}", shimkey);
        shimmerAuthCallbackUrl = shimmerAuthCallbackUrl.replace("{code}", code);
        shimmerAuthCallbackUrl = shimmerAuthCallbackUrl.replace("{state}", state);

        logger.debug("Completing Shimmer Auth: " + shimmerAuthCallbackUrl);

        HttpGet httpGet = new HttpGet(shimmerAuthCallbackUrl);
        CloseableHttpResponse shimmerAuthResponse = getHttpClient().execute(httpGet, getHttpClientContext());
        int statusCode = shimmerAuthResponse.getStatusLine().getStatusCode();
        if(statusCode != 200) {
            logger.debug("Auth Callback Resulted in Response Code " + statusCode);
            throw new Exception("Authorization did not complete");
        }

        logger.debug("Completed Shimmer Auth");
    }

    /**
     * Calls the Shimmer API to retrieve data for a user
     * @param applicationUser the application user for the query
     * @param dateQueries A list of Strings of the format yyyy-MM-dd with start and end date parameters
     * @return the JSON string with the data retrieved from Shimmer or null if nothing was returned
     */
//    public String retrievePatientData(String shimmerId, String shimkey, LocalDate startDate, LocalDate endDate){
    public String retrievePatientData(ApplicationUser applicationUser, List<String> dateQueries) throws Exception{
        logger.debug("Requesting patient data");
        String shimmerDataUrl = System.getenv(SHIMMER_SERVER_URL_BASE_ENV) + SHIMMER_DATA_RANGE_URL;
        shimmerDataUrl = shimmerDataUrl.replace("{shim-key}", applicationUser.getApplicationUserId().getShimKey());
        shimmerDataUrl = shimmerDataUrl.replace("{username}", applicationUser.getShimmerId());
        shimmerDataUrl = shimmerDataUrl.replace("{normalize}", "true");

        LocalDate startDate = null;
        LocalDate endDate = null;
        Map<String, LocalDate> dates = parseDateQueries(dateQueries);
        startDate = dates.get(START_DATE_KEY);
        endDate = dates.get(END_DATE_KEY);

        if(startDate != null) {
            shimmerDataUrl += SHIMMER_START_DATE_URL_PARAM;
            shimmerDataUrl = shimmerDataUrl.replace("{start-date}", startDate.toString());
        }
        if(endDate != null){
            shimmerDataUrl += SHIMMER_END_DATE_URL_PARAM;
            shimmerDataUrl = shimmerDataUrl.replace("{end-date}", endDate.toString());
        }

        logger.debug("Sending data request to " + shimmerDataUrl);
        HttpGet httpGet = new HttpGet(shimmerDataUrl);
        String jsonResponse = processShimmerDataRequest(httpGet);
        if(jsonResponse == null){
            //return a 500 error
            logger.debug("Did not find data");
        }
        logger.debug("Response " + jsonResponse );

        //store the data
        String binaryResourceId = storePatientJson(applicationUser, jsonResponse);

        return binaryResourceId;
    }


    public Map<String, LocalDate> parseDateQueries(List<String> dateQueries) throws Exception{
        LocalDate startDate = null;
        LocalDate endDate = null;
        //if more than two dates throw an error
        if(dateQueries != null && dateQueries.size() > 0 && dateQueries.size() <= 2){
            //convert the passed in dates to Hapi FHIR DateParam objects sorted by date from oldest to most recent
            List<DateParam> dateParams = dateQueries.stream()
                    .map((String dateQuery) -> new DateParam(dateQuery))
                    .sorted(Comparator.comparing(DateParam::getValue))
                    .collect(Collectors.toList());
            if(dateParams.size() == 2){
                startDate = LocalDate.parse(dateParams.get(0).getValueAsString());
                if(dateParams.get(0).getPrefix() != null && dateParams.get(0).getPrefix() != ParamPrefixEnum.GREATERTHAN_OR_EQUALS){
                    throw new Exception("Unsupported FHIR date prefix only GE is supported for start dates.");
                }
                endDate = LocalDate.parse(dateParams.get(1).getValueAsString());
                if(dateParams.get(1).getPrefix() != null && dateParams.get(1).getPrefix() != ParamPrefixEnum.LESSTHAN_OR_EQUALS){
                    throw new Exception("Unsupported FHIR date prefix only LE is supported for end dates.");
                }
            }//end if(dateParams.size() == 2)

            else if(dateParams.size() == 1){
                //figure out if we need to use the start or end date
                ParamPrefixEnum paramPrefix = dateParams.get(0).getPrefix();

                if( paramPrefix != null ) {
                    if (paramPrefix == ParamPrefixEnum.GREATERTHAN_OR_EQUALS) {
                        startDate = LocalDate.parse(dateParams.get(0).getValueAsString());
                    } else if (paramPrefix == ParamPrefixEnum.LESSTHAN_OR_EQUALS) {
                        endDate = LocalDate.parse(dateParams.get(0).getValueAsString());
                    }
                    else{
                        //we don't support the operation
                        throw new Exception("Unsupported FHIR date prefix only GE and LE are supported.");
                    }
                }
                else {
                    //only one date set as both the start and end date
                    startDate = LocalDate.parse(dateParams.get(0).getValueAsString());
                    endDate = LocalDate.parse(dateParams.get(0).getValueAsString());
                }
            }//end else if(dateParams.size() == 1)
        }
        else if(dateQueries != null && dateQueries.size() > 2){
            throw new Exception("No more than two dates can be passed as parameters.");
        }

        Map<String, LocalDate> dateMap = new HashMap<String, LocalDate>();
        dateMap.put(START_DATE_KEY, startDate);
        dateMap.put(END_DATE_KEY, endDate);
        return dateMap;
    }

    /**
     * Writes Shimmer JSON data to the database
     * @param applicationUser the user who owns the data
     * @param jsonResponse the data for the user
     * @return the documentID for the stored shimmer data;
     */
    public String storePatientJson(ApplicationUser applicationUser, String jsonResponse){
        logger.debug("Storing patient data");
        ShimmerData shimmerData = new ShimmerData(applicationUser, jsonResponse);
        shimmerDataRepository.save(shimmerData);
        return shimmerData.getDocumentId();
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
                logger.debug("Shimmer Auth Response: " + responseStr);

                //response JSON {"id":"5b6852e7345e53000bbf6894","stateKey":null,"username":"93c542ab-2705-4526-bf1b-1212d2185087","redirectUri":null,"requestParams":null,"authorizationUrl":null,"clientRedirectUrl":null,"isAuthorized":true,"serializedRequest":null}

                JSONObject responseJson = new JSONObject(responseStr);
                //check if we are already authenticated
                if( !responseJson.getBoolean("isAuthorized") ){
                    authorizationUrl = responseJson.getString("authorizationUrl");
                }
                else{
                    authorizationUrl = System.getenv(ShimmerUtil.OMH_ON_FHIR_CALLBACK_ENV);
                }

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
            CloseableHttpResponse shimmerDataResponse = getHttpClient().execute(request, getHttpClientContext());
            jsonResp = checkShimmerDataResponse(shimmerDataResponse);
        }
        catch(IOException ioe){
            ioe.printStackTrace();
        }
        return jsonResp;
    }

    private String checkShimmerDataResponse(CloseableHttpResponse shimmerAuthResponse) throws IOException {
        logger.debug("Looking for Data");
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
