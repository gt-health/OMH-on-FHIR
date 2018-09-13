package org.gtri.hdap.mdata.jpa.repository;

import org.gtri.hdap.mdata.jpa.entity.ApplicationUser;
import org.gtri.hdap.mdata.jpa.entity.ApplicationUserId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by es130 on 7/9/2018.
 */
public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, ApplicationUserId> {

    ApplicationUser findByShimmerId(String shimmerId);
    ApplicationUser findByApplicationUserIdEhrIdAndApplicationUserIdShimKey(String ehrId, String shimKey);
}