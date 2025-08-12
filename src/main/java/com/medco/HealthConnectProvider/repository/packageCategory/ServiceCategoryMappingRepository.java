package com.medco.HealthConnectProvider.repository.packageCategory;

import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.packageCategory.PackageCategory;
import com.medco.HealthConnectProvider.entity.packageCategory.ServiceCategoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceCategoryMappingRepository extends JpaRepository<ServiceCategoryMapping, Long> {

    Optional<ServiceCategoryMapping> findByMappingUuid(String mappingUuid);

    List<ServiceCategoryMapping> findByContractDetailOrderByPackageCategory_CategoryNameAsc(ContractDetail contractDetail);

    List<ServiceCategoryMapping> findByPackageCategoryOrderByContractDetail_Servicelist_ServiceNameAsc(PackageCategory packageCategory);

    Optional<ServiceCategoryMapping> findByContractDetailAndPackageCategory(ContractDetail contractDetail, PackageCategory packageCategory);

    @Query("SELECT scm FROM ServiceCategoryMapping scm " +
           "WHERE scm.contractDetail.contractDetailUuid = :contractDetailUuid " +
           "AND scm.consumesFromLimit = true " +
           "ORDER BY scm.packageCategory.categoryName ASC")
    List<ServiceCategoryMapping> findActiveByContractDetailUuid(@Param("contractDetailUuid") String contractDetailUuid);

    @Query("SELECT scm FROM ServiceCategoryMapping scm " +
           "WHERE scm.packageCategory.categoryUuid = :categoryUuid " +
           "AND scm.consumesFromLimit = true " +
           "ORDER BY scm.contractDetail.servicelist.serviceName ASC")
    List<ServiceCategoryMapping> findActiveByCategoryUuid(@Param("categoryUuid") String categoryUuid);

    boolean existsByContractDetailAndPackageCategory(ContractDetail contractDetail, PackageCategory packageCategory);

    @Query("SELECT COUNT(scm) FROM ServiceCategoryMapping scm " +
           "WHERE scm.packageCategory = :packageCategory " +
           "AND scm.consumesFromLimit = true")
    long countActiveServicesByCategory(@Param("packageCategory") PackageCategory packageCategory);

    void deleteByContractDetailAndPackageCategory(ContractDetail contractDetail, PackageCategory packageCategory);
}
