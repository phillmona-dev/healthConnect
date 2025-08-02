package com.medco.HealthConnectProvider.repository.service;

import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.entity.services.Servicelist;
import com.medco.HealthConnectProvider.utils.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicelistRepository extends JpaRepository<Servicelist, Long> {

    Page<Servicelist> findByProvider(Optional<Provider> provider, Pageable pageable);

    @Query("SELECT s FROM Servicelist s WHERE s.provider = :provider " +
            "AND (s.serviceName LIKE %:search% OR s.serviceCode LIKE %:search%) " +
            "AND s.isDeleted = false")
    Page<Servicelist> findByProviderAndNameContaining(
            @Param("provider") Optional<Provider> provider,
            @Param("search") String search,
            Pageable pageable);

    boolean existsByServiceName(String serviceName);

    Optional<Servicelist> findByServiceUuid(String string);

    Servicelist findByServiceUuidAndIsDeleted(String serviceUuid, boolean isDeleted);

    @Query("SELECT s FROM Servicelist s WHERE s.provider.providerUuid = :providerUuid " +
            "AND (s.serviceName LIKE %:searchKey% OR s.serviceSubCategory LIKE %:searchKey% " +
            "OR CAST(s.status AS string) LIKE %:searchKey% OR s.serviceCategory LIKE %:searchKey%) " +
            "AND s.isDeleted = false")
    Page<Servicelist> findByProviderUuidAndSearchTerms(
            @Param("providerUuid") String providerUuid,
            @Param("searchKey") String searchKey,
            Pageable pageable);

    Page<Servicelist> findAllByProviderProviderUuidAndIsDeleted(String providerUuid, boolean isDeleted, Pageable pageable);

    List<Servicelist> findAllByProviderProviderUuidAndStatusAndIsDeleted(String providerUuid, Status status, boolean isDeleted);

    Servicelist findByServiceUuidAndProviderProviderUuid(String serviceUuid, String providerUuid);

    List<Service> findByServiceUuidIn(List<String> services);

    @Query("SELECT DISTINCT s.serviceCategory FROM Servicelist s WHERE s.provider = :provider AND s.isDeleted = false ORDER BY s.serviceCategory")
    List<String> findDistinctCategoriesByProvider(@Param("provider") Provider provider);

    Optional<Servicelist> findByServiceNameAndProvider(String serviceName, Provider provider);

    Optional<Servicelist> findByServiceName(String serviceName);

    long countByProviderId(Long id);

    @Query("SELECT s FROM Servicelist s LEFT JOIN FETCH s.provider WHERE s.provider = :provider")
    List<Servicelist> findAllByProviderWithEagerFetch(@Param("provider") Provider provider);

    @Query("SELECT s FROM Servicelist s LEFT JOIN FETCH s.provider WHERE s.provider = :provider AND s.serviceCategory IN :categories")
    List<Servicelist> findByProviderAndServiceCategoryInWithEagerFetch(@Param("provider") Provider provider, @Param("categories") List<String> categories);
}
