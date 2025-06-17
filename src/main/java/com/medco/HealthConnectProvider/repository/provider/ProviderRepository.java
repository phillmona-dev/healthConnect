package com.medco.HealthConnectProvider.repository.provider;

import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.utils.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long>, JpaSpecificationExecutor<Provider> {
    boolean existsByTelephone(String telephone);

    boolean existsByEmail(String email);

    boolean existsByProviderName(String name);

    Optional<Provider> findByProviderUuid(String providerUuid);

    List<Provider> findAllByProviderName(String name);

    Page<Provider> findAllByProviderNameContaining(String searchKey, Pageable pageRequest);

    /**
     * Find providers that are not in contract with a specific payer
     *
     * @param payerUuid UUID of the payer
     * @param pageable Pagination information
     * @return Page of Provider entities
     */
    @Query("SELECT p FROM Provider p WHERE p.isDeleted = false AND p.providerUuid NOT IN " +
            "(SELECT ch.provider.providerUuid FROM ContractHeader ch WHERE ch.payer.payerUuid = :payerUuid AND ch.isDeleted = false)")
    Page<Provider> findAvailableProvidersForPayerNotInContract(@Param("payerUuid") String payerUuid, Pageable pageable);

    /**
     * Find providers that are not in contract with a specific payer, filtered by name
     *
     * @param payerUuid UUID of the payer
     * @param searchKey Search term for filtering by provider name
     * @param pageable Pagination information
     * @return Page of Provider entities
     */
    @Query("SELECT p FROM Provider p WHERE p.isDeleted = false AND p.providerName LIKE %:searchKey% AND p.providerUuid NOT IN " +
            "(SELECT ch.provider.providerUuid FROM ContractHeader ch WHERE ch.payer.payerUuid = :payerUuid AND ch.isDeleted = false)")
    Page<Provider> findAvailableProvidersForPayerWithSearch(
            @Param("payerUuid") String payerUuid,
            @Param("searchKey") String searchKey,
            Pageable pageable);


    @Query("SELECT p FROM Provider p WHERE p.isDeleted = false " +
            "AND p.status = 'ACTIVE' " +
            "AND (p.providerName LIKE %:search% OR p.email LIKE %:search% OR p.telephone LIKE %:search%) " +
            "AND p.providerUuid NOT IN (" +
            "    SELECT c.provider.providerUuid FROM ContractHeader c " +
            "    WHERE c.payer.payerUuid = :payerUuid " +
            "    AND c.status = :status " +
            "    AND c.isDeleted = false" +
            ")")
    Page<Provider> findAvailableProvidersForPayer(
            @Param("payerUuid") String payerUuid,
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable);


    /**
     * Find providers with multiple filter criteria
     */
    @Query("SELECT p FROM Provider p WHERE p.isDeleted = false " +
            "AND (:searchKey IS NULL OR :searchKey = '' OR " +
            "    p.providerName LIKE %:searchKey% OR " +
            "    p.email LIKE %:searchKey% OR " +
            "    p.telephone LIKE %:searchKey%) " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND (:category IS NULL OR :category = '' OR p.category = :category) " +
            "AND (:providerName IS NULL OR :providerName = '' OR p.providerName = :providerName) " +
            "AND (:tinNumber IS NULL OR p.tinNumber = :tinNumber) " +
            "AND (:level IS NULL OR :level = '' OR p.level = :level)")
    Page<Provider> findProvidersWithFilters(
            @Param("searchKey") String searchKey,
            @Param("status") Status status,
            @Param("category") String category,
            @Param("providerName") String providerName,
            @Param("tinNumber") String tinNumber,
            @Param("level") String level,
            Pageable pageable);
}
