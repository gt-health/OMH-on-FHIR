package org.gtri.hdap.mdata.jpa.repository;

import org.gtri.hdap.mdata.jpa.entity.ApplicationUser;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by es130 on 7/9/2018.
 */
public interface ApplicationUserRepository extends CrudRepository<ApplicationUser, Long> {

//    List<ApplicationUser> findByEhrId(String ehrId);
//    List<ApplicationUser> findByShimmerId(String shimmerId);
}