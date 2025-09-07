package com.medco.HealthConnectProvider.entity.packageCategory;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * Entity listener for ServiceCategoryMapping to ensure it's created after ContractDetail
 */
public class ServiceCategoryMappingEntityListener {

    @PrePersist
    public void prePersist(ServiceCategoryMapping mapping) {
        if (mapping.getMappingUuid() == null) {
            mapping.setMappingUuid(java.util.UUID.randomUUID().toString());
        }
    }

    @PreUpdate
    public void preUpdate(ServiceCategoryMapping mapping) {
        // Any pre-update logic if needed
    }
}