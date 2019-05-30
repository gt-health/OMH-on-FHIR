package org.gtri.hdap.mdata.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.gtri.hdap.mdata.jpa.entity.ApplicationUser;
import org.gtri.hdap.mdata.jpa.repository.ApplicationUserRepository;
import org.gtri.hdap.mdata.service.ShimmerAuthService;
import org.gtri.hdap.mdata.service.ShimmerResponse;
import org.gtri.hdap.mdata.service.ShimmerService;
import org.gtri.hdap.mdata.util.ShimmerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
@Api(value = "shimmerauthcontroller", description = "Endpoints to handle authentication of the Shimmer.")
public class ShimmerAuthController {

    private final Logger logger = LoggerFactory.getLogger(ShimmerAuthController.class);

    @Autowired
    private ShimmerAuthService shimmerAuthService;
    @Autowired
    private ShimmerService shimmerService;
    @Autowired
    private SessionMetaData sessionMetaData;
    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    /**
     * Make a request to http://<shimmer-host>:8083/authorize/{shimKey}?username={userId}
     * @param model
     * @param ehrId the ID of the patient in the EHR
     * @param shimkey the ID for the patient in Shimmer
     * @return
     */
    @ApiOperation(value="Authenticate an EHR user to use a specific shim with the Shimmer library.")
    @GetMapping("/shimmerAuthentication")
    public ModelAndView authenticateWithShimmer(ModelMap model,
                                                @RequestParam(name="ehrId", required=true) String ehrId,
                                                @RequestParam(name="shimkey", required=true) String shimkey
    ){
        logger.debug("Entering method authenticateWithShimmer");
        logger.debug("Trying to connect to " + shimkey + " API");
        // Make a request to http://<shimmer-host>:8083/authorize/{shimKey}?username={userId}
        // The shimKey path parameter should be one of the keys listed below, e.g. fitbit
        // for example https://gt-apps.hdap.gatech.edu:8083/authorize/fitbit?username={userId}
        // The username query parameter can be set to any unique identifier you'd like to use to identify the user.

        logger.debug("calling getShimmerId");
        String userShimmerId = shimmerAuthService.getShimmerId(ehrId, shimkey);
        logger.debug("called getShimmerId");
        //add the shimmer id to the model
        model.addAttribute("shimmerId", userShimmerId);
        this.sessionMetaData.setShimmerId(userShimmerId);

        ShimmerResponse shimmerResponse = shimmerService.requestShimmerAuthUrl(userShimmerId, shimkey);
        String oauthAuthUrl = null;
        if( shimmerResponse.getResponseCode() == HttpStatus.OK.value()){
            oauthAuthUrl = shimmerResponse.getResponseData();
        }
        else{
            //did not get a response URL
            String callbackUrl= "redirect:" + System.getenv(ShimmerUtil.OMH_ON_FHIR_CALLBACK_ENV);
            model.addAttribute("loginSuccess", false);
            logger.debug("HTTP " + shimmerResponse.getResponseCode() + " returned by Shimmer Auth response. Could not authorize shimmer user " +
                    shimmerResponse.getResponseData() + "Error with Authentication. Redirecting to: " + callbackUrl);
            return new ModelAndView(callbackUrl, model);
        }

        logger.debug("Finished connection to " + shimkey + " API");

        //If the returned oauthAuthUrl equals the final callback URL for UI then the user has already
        //linked the EHR user to their device account via shimmer. Update the model to contain
        //loginSuccess true. The shimmerID for the model was set above so no need to set it again.
        if( oauthAuthUrl.equals(System.getenv(ShimmerUtil.OMH_ON_FHIR_CALLBACK_ENV))) {
            logger.debug("User already approved. Forwarding to login callback UI page");
            model.addAttribute("loginSuccess", true);
        }

        String redirectUrl = "redirect:" + oauthAuthUrl;
        //THIS IS A HACK, check and remove  org.springframework.validation.BindingResult.shimmerId from the model
        model.remove("org.springframework.validation.BindingResult.shimmerId");

        ModelAndView mvToReturn = new ModelAndView(redirectUrl, model);

        return mvToReturn;
    }

    @ApiOperation(value="Callback method for Shimmer to use during user authentication.")
    @GetMapping("/authorize/{shimkey}/callback")
    public ModelAndView handleShimmerOauthCallback(ModelMap model,
                                                   @PathVariable String shimkey,
                                                   @RequestParam(name="code") String code,
                                                   @RequestParam(name="state") String state
    ){
        logger.debug("Handling successful " + shimkey + " auth redirect");
        logger.debug("MODEL shimmer id " + model.get("shimmerId"));
        logger.debug("Passed in shimmer id " + sessionMetaData.getShimmerId());
        logger.debug("Code " + code);
        logger.debug("State " + state);

        String omhOnFhirUi;

        //TODO: Why is this call to the shimmer API not working for Fitbit?
        try {
            shimmerService.completeShimmerAuth(shimkey, code, state);
        }
        catch(Exception e){
            e.printStackTrace();
            omhOnFhirUi = "redirect:" + System.getenv(ShimmerUtil.OMH_ON_FHIR_CALLBACK_ENV);
            model.addAttribute("loginSuccess", false);
            logger.debug("Error with Authentication. Redirecting to: " + omhOnFhirUi);
            return new ModelAndView(omhOnFhirUi, model);
        }

        ApplicationUser applicationUser = applicationUserRepository.findByShimmerId(sessionMetaData.getShimmerId());
        if(applicationUser != null){
            applicationUserRepository.save(applicationUser);
        }
        else{
            omhOnFhirUi = "redirect:" + System.getenv(ShimmerUtil.OMH_ON_FHIR_CALLBACK_ENV);
            model.addAttribute("loginSuccess", false);
            logger.debug("Could not find Shimmer ID for user. Redirecting to: " + omhOnFhirUi);
            return new ModelAndView(omhOnFhirUi, model);
        }

        omhOnFhirUi = "redirect:" + System.getenv(ShimmerUtil.OMH_ON_FHIR_CALLBACK_ENV);
        model.addAttribute("loginSuccess", true);
        model.addAttribute("shimmerId", sessionMetaData.getShimmerId());
        logger.debug("Redirecting to: " + omhOnFhirUi);
        return new ModelAndView(omhOnFhirUi, model);
    }

}
