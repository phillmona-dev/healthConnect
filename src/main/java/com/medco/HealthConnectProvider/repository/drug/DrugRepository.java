package com.medco.HealthConnectProvider.repository.drug;

import com.medco.HealthConnectProvider.entity.drug.Drug;
import com.medco.HealthConnectProvider.utils.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrugRepository extends JpaRepository<Drug, Long> {

    Optional<Drug> findByDrugUuid(String drugUuid);

    boolean existsByDrugName(String drugName);

    Page<Drug> findAllByProviderProviderUuidAndIsDeleted(String providerUuid, boolean isDeleted, Pageable pageable);

    @Query("SELECT d FROM Drug d WHERE d.provider.providerUuid = :providerUuid AND " +
            "(LOWER(d.drugName) LIKE LOWER(CONCAT('%', :searchKey, '%')) OR " +
            "LOWER(d.drugCode) LIKE LOWER(CONCAT('%', :searchKey, '%')) OR " +
            "LOWER(d.category) LIKE LOWER(CONCAT('%', :searchKey, '%')) OR " +
            "LOWER(d.subCategory) LIKE LOWER(CONCAT('%', :searchKey, '%')))")
    Page<Drug> findByProviderUuidAndSearchTerms(String providerUuid, String searchKey, Pageable pageable);

    List<Drug> findAllByProviderProviderUuidAndStatusAndIsDeleted(String providerUuid, Status status, boolean isDeleted);

    @Query("SELECT d FROM Drug d WHERE d.provider.providerUuid = :providerUuid AND d.isDeleted = false AND " +
            "(LOWER(d.drugName) LIKE LOWER(CONCAT('%', :searchKey, '%')) OR " +
            "LOWER(d.drugCode) LIKE LOWER(CONCAT('%', :searchKey, '%')) OR " +
            "LOWER(d.category) LIKE LOWER(CONCAT('%', :searchKey, '%')) OR " +
            "LOWER(d.subCategory) LIKE LOWER(CONCAT('%', :searchKey, '%')))")
    Page<Drug> findByProviderUuidAndSearchTermsAndIsDeletedFalse(
            @Param("providerUuid") String providerUuid,
            @Param("searchKey") String searchKey,
            Pageable pageable);

    Page<Drug> findAllByProviderProviderUuidAndIsDeletedFalse(String providerUuid, Pageable pageable);

}
