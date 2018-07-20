package org.gtri.hdap.mdata.jpa.repository;

import org.gtri.hdap.mdata.jpa.entity.ShimmerData;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by es130 on 7/9/2018.
 */
public interface ShimmerDataRepository extends CrudRepository<ShimmerData, Long> {
    ShimmerData findByDocumentId(String documentId);
}
