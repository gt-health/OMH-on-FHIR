package org.gtri.hdap.mdata.common.jpa.repository;

import org.gtri.hdap.mdata.common.jpa.entity.ApplicationUser;
import org.gtri.hdap.mdata.common.jpa.entity.ApplicationUserId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by es130 on 7/9/2018.
 */
public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, ApplicationUserId> {

    ApplicationUser findByShimmerId(String shimmerId);
    ApplicationUser findByApplicationUserIdEhrIdAndApplicationUserIdShimKey(String ehrId, String shimKey);
}