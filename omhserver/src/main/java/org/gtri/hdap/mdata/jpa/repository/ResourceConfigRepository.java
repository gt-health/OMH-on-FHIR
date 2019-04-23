package org.gtri.hdap.mdata.jpa.repository;

import org.gtri.hdap.mdata.jpa.entity.ResourceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceConfigRepository extends JpaRepository<ResourceConfig, String> {
    ResourceConfig findOneByResourceId(String resourceId);
}
