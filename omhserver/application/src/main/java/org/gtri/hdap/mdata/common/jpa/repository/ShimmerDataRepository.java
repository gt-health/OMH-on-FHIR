package org.gtri.hdap.mdata.common.jpa.repository;

import org.gtri.hdap.mdata.common.jpa.entity.ShimmerData;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by es130 on 7/9/2018.
 */
public interface ShimmerDataRepository extends CrudRepository<ShimmerData, Long> {
    ShimmerData findByDocumentId(String documentId);
}
