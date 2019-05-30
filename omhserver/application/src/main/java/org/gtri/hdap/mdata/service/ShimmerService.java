package org.gtri.hdap.mdata.service;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
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
import org.gtri.hdap.mdata.jpa.repository.ShimmerDataRepository;
import org.gtri.hdap.mdata.util.ShimmerUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
//public static String SHIMMER_STEP_COUNT_RANGE_URL = "/data/{shim-key}/physical_activity?username={username}&normalize={normalize}";
    public static String SHIMMER_STEP_COUNT_RANGE_URL = "/data/{shim-key}/{omh-resource-id}?username={username}&normalize={normalize}";
    public static String SHIMMER_ACTIVITY_RANGE_URL = "/data/{shim-key}/{omh-resource-id}?username={username}&normalize={normalize}";
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

    @Autowired
    private ShimmerDataRepository shimmerDataRepository;

    /*========================================================================*/
    /* Public Methods */
    /*========================================================================*/

    public ShimmerResponse requestShimmerAuthUrl(String shimmerId, String shimkey){
        String shimmerAuthUrl = System.getenv(SHIMMER_SERVER_URL_BASE_ENV) + SHIMMER_AUTH_URL;
        String shimmerRedirectUrl = System.getenv(SHIMMER_SERVER_REDIRECT_URL_ENV);
        shimmerAuthUrl = shimmerAuthUrl.replace("{shim-key}", shimkey);
        shimmerAuthUrl = shimmerAuthUrl.replace("{username}", shimmerId);
        shimmerRedirectUrl = shimmerRedirectUrl.replace("{shim-key}", shimkey);
        shimmerAuthUrl = shimmerAuthUrl.replace("{redirect-url}", shimmerRedirectUrl);
        logger.debug("Sending authorization request to " + shimmerAuthUrl);
        HttpGet httpGet = new HttpGet(shimmerAuthUrl);
        ShimmerResponse shimmerResponse = processShimmerAuthRequest(httpGet);
        return shimmerResponse;
    }

    public void completeShimmerAuth(String shimkey, String code, String state) throws Exception{
        String shimmerAuthCallbackUrl = System.getenv(SHIMMER_SERVER_URL_BASE_ENV) + SHIMMER_AUTH_CALLBACK;
        shimmerAuthCallbackUrl = shimmerAuthCallbackUrl.replace("{shim-key}", shimkey);
        shimmerAuthCallbackUrl = shimmerAuthCallbackUrl.replace("{code}", code);
        shimmerAuthCallbackUrl = shimmerAuthCallbackUrl.replace("{state}", state);

        logger.debug("Completing Shimmer Auth: " + shimmerAuthCallbackUrl);

        HttpGet httpGet = new HttpGet(shimmerAuthCallbackUrl);
        CloseableHttpClient httpClient = createHttpClient();
        HttpClientContext httpClientContext = HttpClientContext.create();
        CloseableHttpResponse shimmerAuthResponse = httpClient.execute(httpGet, httpClientContext);
        int statusCode;
        try {
            statusCode = shimmerAuthResponse.getStatusLine().getStatusCode();
        }
        finally {
            shimmerAuthResponse.close();
        }
        if (statusCode != 200) {
            logger.debug("Auth Callback Resulted in Response Code " + statusCode);
            throw new Exception("Authorization did not complete: " +  EntityUtils.toString(shimmerAuthResponse.getEntity()));
        }

        logger.debug("Completed Shimmer Auth");
    }

    /**
     * Calls the Shimmer API to retrieve data for a user
     * @param applicationUser the application user for the query
     * @param dateQueries A list of Strings of the format yyyy-MM-dd with start and end date parameters
     * @return ShimmerResponse with details of the search.
     */
    public ShimmerResponse retrievePatientData(ApplicationUser applicationUser, List<String> dateQueries, String omhResourceId){
        logger.debug("Requesting patient data");
        ShimmerResponse shimmerResponse = retrieveShimmerData(SHIMMER_ACTIVITY_RANGE_URL, applicationUser, dateQueries, omhResourceId);
        //        ShimmerResponse shimmerResponse = retrieveShimmerData(SHIMMER_ACTIVITY_RANGE_URL, applicationUser, dateQueries);
        return shimmerResponse;
    }

    /**
     * Persist data returned in a ShimmerResponse for a specific user
     * @param applicationUser the application user for the query
     * @param shimmerResponse the ShimmerResponse to process
     * @return the id to use to retrieve the stored data
     */
    public String writePatientData(ApplicationUser applicationUser, ShimmerResponse shimmerResponse){
        String binaryRefId = "";
        logger.debug("Response " + shimmerResponse.getResponseData());
        if(shimmerResponse.getResponseData() != null){
            //store the data
            binaryRefId = storePatientJson(applicationUser, shimmerResponse.getResponseData());
        }
        logger.debug("Binary ID for patient data: " + binaryRefId);
        return binaryRefId;
    }


    /**
     * Calls the Shimmer API to retrieve data for a user
     * @param applicationUser the application user for the query
     * @param dateQueries A list of Strings of the format yyyy-MM-dd with start and end date parameters
     * @return ShimmerResponse object with response details.
     */
    public ShimmerResponse retrieveShimmerData(
        String shimmerDataUrlFragment,
        ApplicationUser applicationUser,
        List<String> dateQueries,
        String omhResource
    ){
        logger.debug("Querying Shimmer");
        String shimmerDataUrl = System.getenv(SHIMMER_SERVER_URL_BASE_ENV) + shimmerDataUrlFragment;
        shimmerDataUrl = shimmerDataUrl.replace("{shim-key}", applicationUser.getApplicationUserId().getShimKey());
        shimmerDataUrl = shimmerDataUrl.replace("{username}", applicationUser.getShimmerId());
        shimmerDataUrl = shimmerDataUrl.replace("{normalize}", "true");
        shimmerDataUrl = shimmerDataUrl.replace("{omh-resource-id}", omhResource);

        LocalDate startDate = null;
        LocalDate endDate = null;
        if( dateQueries != null ) {
            try {
                Map<String, LocalDate> dates = parseDateQueries(dateQueries);
                startDate = dates.get(START_DATE_KEY);
                endDate = dates.get(END_DATE_KEY);
            } catch(Exception e) {
                String errorResponse = "{\"status\":" + HttpStatus.SC_BAD_REQUEST + ",\"exception\":\"" + e.getMessage() + "\"}";
                return new ShimmerResponse(HttpStatus.SC_BAD_REQUEST, errorResponse);
            }
        }
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
        ShimmerResponse shimmerResponse = processShimmerDataRequest(httpGet);
        logger.debug("Completed query to Shimmer");
        return shimmerResponse;
    }

    public boolean datesValid(LocalDate start, LocalDate end) {
        if (start.compareTo(end) > 0) {
            return false;
        }
        if (start.until(end, ChronoUnit.YEARS) >= 3) {
            return false;
        }
        return true;
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
                    throw new UnsupportedFhirDatePrefixException("Unsupported FHIR date prefix only GE is supported for start dates.");
                }
                endDate = LocalDate.parse(dateParams.get(1).getValueAsString());
                if(dateParams.get(1).getPrefix() != null && dateParams.get(1).getPrefix() != ParamPrefixEnum.LESSTHAN_OR_EQUALS){
                    throw new UnsupportedFhirDatePrefixException("Unsupported FHIR date prefix only LE is supported for end dates.");
                }
                // validate dates queries
                if (!datesValid(startDate, endDate)) {
                    throw new ShimmerQueryParsingException(
                        "Dates are invalid. Ensure start date is before end date, and time margin is smaller than 3 years."
                    );
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
                        throw new UnsupportedFhirDatePrefixException("Unsupported FHIR date prefix only GE and LE are supported.");
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
            throw new UnsupportedFhirDatePrefixException("No more than two dates can be passed as parameters.");
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

    private ShimmerResponse processShimmerAuthRequest(HttpUriRequest request){
        CloseableHttpClient httpClient = createHttpClient();
        HttpClientContext httpClientContext = HttpClientContext.create();
        ShimmerResponse shimmerResponse;
        try {
            CloseableHttpResponse shimmerAuthResponse = httpClient.execute(request, httpClientContext);
            //checkShimmerAuthResponse handles closing shimmerAuthResponse
            try {
                shimmerResponse = checkShimmerAuthResponse(shimmerAuthResponse);
            }
            catch(IOException ioe){
                logger.error("Error processing Shimmer response", ioe);
                String errorResponse = "{\"status\":" + org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value() + ",\"exception\":\"Could not complete Shimmer request\"}";
                shimmerResponse = new ShimmerResponse(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse);
            }
        }
        catch(IOException ioe){
            String errorResponse = "{\"status\":" + org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value() + ",\"exception\":\"Could not process Shimmer response\"}";
            shimmerResponse = new ShimmerResponse(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse);
        }
        return shimmerResponse;
    }

    /**
     * Creates a {@link CloseableHttpClient} to use in the application
     * @return
     */
    private CloseableHttpClient createHttpClient(){
        //TODO: Fix to use non-deprecated code
        logger.debug("Creating HTTP Client");
        RequestConfig globalConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .build();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(globalConfig)
//                .setSSLSocketFactory(getSslsf())
//                .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                .build();
        logger.debug("Returning created HTTP Client");
        return httpClient;
    }

    private ShimmerResponse checkShimmerAuthResponse(CloseableHttpResponse shimmerAuthResponse) throws IOException {
        String shimmerResponseData = null;
        ShimmerResponse shimmerResponse;
        try {
            int statusCode = shimmerAuthResponse.getStatusLine().getStatusCode();
            //All we need is the cookie, which is managed as part of the HTTPContext
            //for now ignore the content of the response. At a later date process
            //the JSON. It contains permission and user metadata.
            HttpEntity responseEntity = shimmerAuthResponse.getEntity();
            //get the json from the response and get the auth URL to redirect the user
            String responseStr = EntityUtils.toString(responseEntity);

            shimmerResponse = new ShimmerResponse(statusCode, responseStr);

            logger.debug("Response Code " + statusCode);
            if(statusCode == 200) {
                //All we need is the cookie, which is managed as part of the HTTPContext
                //for now ignore the content of the response. At a later date process
                //the JSON. It contains permission and user metadata.
//                HttpEntity responseEntity = shimmerAuthResponse.getEntity();
                //get the json from the response and get the auth URL to redirect the user
//                String responseStr = EntityUtils.toString(responseEntity);
                logger.debug("Shimmer Auth Response: " + responseStr);

                //response JSON {"id":"5b6852e7345e53000bbf6894","stateKey":null,"username":"93c542ab-2705-4526-bf1b-1212d2185087","redirectUri":null,"requestParams":null,"authorizationUrl":null,"clientRedirectUrl":null,"isAuthorized":true,"serializedRequest":null}

                JSONObject responseJson = new JSONObject(responseStr);
                //check if we are already authenticated
                if( !responseJson.getBoolean("isAuthorized") ){
                    logger.debug("User is not authorized");
                    shimmerResponseData = responseJson.getString("authorizationUrl");
                }
                else{
                    logger.debug("User is authorized");
                    shimmerResponseData = System.getenv(ShimmerUtil.OMH_ON_FHIR_CALLBACK_ENV);
                }

                //set the URL as
                shimmerResponse.setResponseData(shimmerResponseData);

                logger.debug("Authorization URL " + shimmerResponseData);
            }
        } finally {
            shimmerAuthResponse.close();
        }
        return shimmerResponse;
    }

    private ShimmerResponse processShimmerDataRequest(HttpUriRequest request){
        ShimmerResponse shimmerResponse;
        try {
            logger.debug("Sending Shimmer Data Request");
            CloseableHttpClient httpClient = createHttpClient();
            HttpClientContext httpClientContext = HttpClientContext.create();
            CloseableHttpResponse shimmerDataResponse = httpClient.execute(request, httpClientContext);
            logger.debug("Received shimmer data");
            //checkShimmerDataResponse closes the shimmerDataResponse
            shimmerResponse = checkShimmerDataResponse(shimmerDataResponse);
        }
        catch(IOException ioe){
            logger.error("Error processing Shimmer response", ioe);
            String errorResponse = "{\"status\":" + org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value() + ",\"exception\":\"Could not process Shimmer response\"}";
            shimmerResponse = new ShimmerResponse(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse);
        }
        return shimmerResponse;
    }

    private ShimmerResponse checkShimmerDataResponse(CloseableHttpResponse shimmerAuthResponse) throws IOException {
        logger.debug("Looking for Data");
        String jsonResp = "";
        ShimmerResponse shimmerResponse;
        try {
            int statusCode = shimmerAuthResponse.getStatusLine().getStatusCode();
            HttpEntity responseEntity = shimmerAuthResponse.getEntity();
            jsonResp = EntityUtils.toString(responseEntity);

            logger.debug("Response Code " + statusCode);
            logger.debug("Data Response: " + jsonResp);
            shimmerResponse = new ShimmerResponse(statusCode, jsonResp);
//            if(statusCode == 200) {
//                //All we need is the cookie, which is managed as part of the HTTPContext
//                //for now ignore the content of the response. At a later date process
//                //the JSON. It contains permission and user metadata.
//
//                //get the json from the response and get the auth URL to redirect the user
//                jsonResp = EntityUtils.toString(responseEntity);
//                logger.debug("Data Response: " + jsonResp);
//            }
//            else{
//                logger.debug("Different Response Code " + statusCode);
//                jsonResp = EntityUtils.toString(responseEntity);
//                logger.debug(EntityUtils.toString(responseEntity));
//            }

        }
        catch(Exception ioe){
            logger.error("Error parsing response Entity", ioe);
            String errorResponse = "{\"status\":500,\"exception\":\"Could not parse response entity of Shimmer response\"}";
            shimmerResponse = new ShimmerResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, errorResponse);
        }
        finally {
            shimmerAuthResponse.close();
        }

        return shimmerResponse;
    }
}
