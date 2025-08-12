package com.medco.HealthConnectProvider.repository.packageCategory;

import com.medco.HealthConnectProvider.entity.packageCategory.PackageCategoryLimit;
import com.medco.HealthConnectProvider.entity.packageCategory.PackageCategoryUsage;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PackageCategoryUsageRepository extends JpaRepository<PackageCategoryUsage, Long> {

    Optional<PackageCategoryUsage> findByUsageUuid(String usageUuid);

    List<PackageCategoryUsage> findByInsuredPersonAndCategoryLimitOrderByServiceDateDesc(
            Insured insuredPerson, PackageCategoryLimit categoryLimit);

    @Query("SELECT pcu FROM PackageCategoryUsage pcu " +
           "WHERE pcu.insuredPerson = :insuredPerson " +
           "AND pcu.categoryLimit = :categoryLimit " +
           "AND pcu.serviceDate BETWEEN :startDate AND :endDate " +
           "ORDER BY pcu.serviceDate DESC")
    List<PackageCategoryUsage> findByInsuredPersonAndCategoryLimitAndPeriod(
            @Param("insuredPerson") Insured insuredPerson,
            @Param("categoryLimit") PackageCategoryLimit categoryLimit,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(pcu.usedAmount), 0) FROM PackageCategoryUsage pcu " +
           "WHERE pcu.insuredPerson = :insuredPerson " +
           "AND pcu.categoryLimit = :categoryLimit " +
           "AND pcu.serviceDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalUsedAmountByInsuredAndCategoryAndPeriod(
            @Param("insuredPerson") Insured insuredPerson,
            @Param("categoryLimit") PackageCategoryLimit categoryLimit,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(pcu.usedQuantity), 0) FROM PackageCategoryUsage pcu " +
           "WHERE pcu.insuredPerson = :insuredPerson " +
           "AND pcu.categoryLimit = :categoryLimit " +
           "AND pcu.serviceDate BETWEEN :startDate AND :endDate")
    Double getTotalUsedQuantityByInsuredAndCategoryAndPeriod(
            @Param("insuredPerson") Insured insuredPerson,
            @Param("categoryLimit") PackageCategoryLimit categoryLimit,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(pcu) FROM PackageCategoryUsage pcu " +
           "WHERE pcu.insuredPerson = :insuredPerson " +
           "AND pcu.categoryLimit = :categoryLimit " +
           "AND pcu.serviceDate BETWEEN :startDate AND :endDate")
    Integer getTotalUsedVisitsByInsuredAndCategoryAndPeriod(
            @Param("insuredPerson") Insured insuredPerson,
            @Param("categoryLimit") PackageCategoryLimit categoryLimit,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<PackageCategoryUsage> findByClaimUuid(String claimUuid);

    List<PackageCategoryUsage> findByProvidedServiceUuid(String providedServiceUuid);

    @Query("SELECT pcu FROM PackageCategoryUsage pcu " +
           "WHERE pcu.insuredPerson.insuredUuid = :insuredUuid " +
           "AND pcu.categoryLimit.packageCategory.categoryUuid = :categoryUuid " +
           "ORDER BY pcu.serviceDate DESC")
    List<PackageCategoryUsage> findByInsuredUuidAndCategoryUuid(
            @Param("insuredUuid") String insuredUuid,
            @Param("categoryUuid") String categoryUuid);
}
