package com.medco.HealthConnectProvider.repository.service;

import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.entity.services.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    Page<Service> findByProvider(Optional<Provider> provider, Pageable pageable);

    @Query("SELECT s FROM Service s WHERE s.provider = :provider " +
            "AND (s.serviceName LIKE %:search% OR s.serviceCode LIKE %:search%) " +
            "AND s.isDeleted = false")
    Page<Service> findByProviderAndNameContaining(
            @Param("provider") Optional<Provider> provider,
            @Param("search") String search,
            Pageable pageable);
}
