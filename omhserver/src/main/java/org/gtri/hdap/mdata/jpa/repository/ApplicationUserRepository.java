package org.gtri.hdap.mdata.jpa.repository;

import org.gtri.hdap.mdata.jpa.entity.ApplicationUser;
import org.gtri.hdap.mdata.jpa.entity.ApplicationUserId;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Created by es130 on 7/9/2018.
 */
//public interface ApplicationUserRepository extends CrudRepository<ApplicationUser, Long> {
public interface ApplicationUserRepository extends CrudRepository<ApplicationUser, ApplicationUserId> {

    ApplicationUser findByShimmerId(String shimmerId);
}