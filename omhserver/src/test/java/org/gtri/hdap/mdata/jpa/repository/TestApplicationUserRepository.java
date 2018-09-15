package org.gtri.hdap.mdata.jpa.repository;

import org.gtri.hdap.mdata.jpa.entity.ApplicationUser;
import org.gtri.hdap.mdata.jpa.entity.ApplicationUserId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertTrue;

/**
 * Created by es130 on 9/12/2018.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@TestPropertySource(locations = "classpath:test.application.properties")
public class TestApplicationUserRepository {

    private final Logger logger = LoggerFactory.getLogger(TestApplicationUserRepository.class);

    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    @Test
    public void testRepository() throws Exception{
        logger.debug("========== Entering testRepository ==========");
        String ehrId = "123";
        String shimmerId = "345";
        String shimkey = "fitbit";
        ApplicationUserId applicationUserid = new ApplicationUserId(ehrId, shimkey);
        ApplicationUser applicationUser = new ApplicationUser(applicationUserid, shimmerId);
        applicationUserRepository.save(applicationUser);

        logger.debug("Printing application users");

        for(ApplicationUser applicationUser1 : applicationUserRepository.findAll() ){
            logger.debug("User EHR ID: " + applicationUser1.getApplicationUserId().getEhrId() );
            logger.debug("User SHIM KEY: " + applicationUser1.getApplicationUserId().getShimKey() );
            logger.debug("Shim ID: " + applicationUser1.getShimmerId());
        }
        logger.debug("Finished Printing users");

        //now see if we can find the user
        assertTrue(applicationUserRepository.findById(applicationUserid).isPresent());
        ApplicationUser foundUser = applicationUserRepository.findById(applicationUserid).get();
        assertTrue(foundUser.getApplicationUserId().getEhrId().equals(applicationUserid.getEhrId()));
        assertTrue(foundUser.getApplicationUserId().getShimKey().equals(applicationUserid.getShimKey()));

        assertTrue(applicationUserRepository.findByApplicationUserIdEhrIdAndApplicationUserIdShimKey(ehrId, shimkey) != null);
        logger.debug("========== Exiting testRepository ==========");
    }
}
