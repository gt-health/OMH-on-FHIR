package org.gtri.hdap.mdata.common.service;

import org.gtri.hdap.mdata.common.jpa.entity.ApplicationUser;
import org.gtri.hdap.mdata.common.jpa.entity.ApplicationUserId;
import org.gtri.hdap.mdata.common.jpa.repository.ApplicationUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ShimmerAuthService {

    @Autowired
    ApplicationUserRepository applicationUserRepository;

    private Logger logger = LoggerFactory.getLogger(ShimmerAuthService.class);

    public ApplicationUser createNewApplicationUser(ApplicationUserId applicationUserId){
        logger.debug("User does not exist, creating");
        String shimmerId = UUID.randomUUID().toString();
        ApplicationUser newUser = new ApplicationUser(applicationUserId, shimmerId);
        applicationUserRepository.save(newUser);
        //force a flush
        applicationUserRepository.flush();
        logger.debug("finished creating user");
        return newUser;
    }

    public String getShimmerId(String ehrId, String shimkey){
        logger.debug("Checking User EHR ID: [" + ehrId + "] ShimKey: [" + shimkey + "]");
        ApplicationUserId applicationUserId = new ApplicationUserId(ehrId, shimkey);
        //debug info
        logger.debug("Find by ID " + applicationUserRepository.findById(applicationUserId).isPresent());
        ApplicationUser user;
        Optional<ApplicationUser> applicationUserOptional = applicationUserRepository.findById(applicationUserId);
        if(applicationUserOptional.isPresent()){
            logger.debug("Found the user");
            user = applicationUserOptional.get();
        }
        else{
            logger.debug("Did not find user. Creating new one");
            user = createNewApplicationUser(applicationUserId);
        }
        String shimmerId = user.getShimmerId();
        logger.debug("Returning shimmer id: " + shimmerId);
        return shimmerId;
    }

}
