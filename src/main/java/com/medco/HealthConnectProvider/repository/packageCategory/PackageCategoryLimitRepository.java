package com.medco.HealthConnectProvider.repository.packageCategory;

import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.packageCategory.PackageCategory;
import com.medco.HealthConnectProvider.entity.packageCategory.PackageCategoryLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PackageCategoryLimitRepository extends JpaRepository<PackageCategoryLimit, Long> {

    Optional<PackageCategoryLimit> findByLimitUuid(String limitUuid);

    List<PackageCategoryLimit> findByContractHeaderAndIsActiveTrueOrderByPackageCategory_CategoryNameAsc(ContractHeader contractHeader);

    List<PackageCategoryLimit> findByPackageCategoryAndContractHeaderAndIsActiveTrueOrderByResetDateDesc(
            PackageCategory packageCategory, ContractHeader contractHeader);

    Optional<PackageCategoryLimit> findByPackageCategoryAndContractHeaderAndIsActiveTrueAndResetDateAfter(
            PackageCategory packageCategory, ContractHeader contractHeader, LocalDate currentDate);

    @Query("SELECT pcl FROM PackageCategoryLimit pcl " +
           "WHERE pcl.contractHeader.contractHeaderUuid = :contractUuid " +
           "AND pcl.isActive = true " +
           "ORDER BY pcl.packageCategory.categoryName ASC")
    List<PackageCategoryLimit> findActiveByContractUuid(@Param("contractUuid") String contractUuid);

    @Query("SELECT pcl FROM PackageCategoryLimit pcl " +
           "WHERE pcl.packageCategory.categoryUuid = :categoryUuid " +
           "AND pcl.contractHeader.contractHeaderUuid = :contractUuid " +
           "AND pcl.isActive = true " +
           "AND pcl.resetDate > :currentDate " +
           "ORDER BY pcl.resetDate DESC")
    Optional<PackageCategoryLimit> findActiveLimitByCategoryAndContract(
            @Param("categoryUuid") String categoryUuid,
            @Param("contractUuid") String contractUuid,
            @Param("currentDate") LocalDate currentDate);

    @Query("SELECT pcl FROM PackageCategoryLimit pcl " +
           "WHERE pcl.resetDate <= :currentDate " +
           "AND pcl.isActive = true")
    List<PackageCategoryLimit> findExpiredLimits(@Param("currentDate") LocalDate currentDate);

    boolean existsByPackageCategoryAndContractHeaderAndIsActiveTrueAndResetDateAfter(
            PackageCategory packageCategory, ContractHeader contractHeader, LocalDate currentDate);
}
