package com.medco.HealthConnectProvider.repository.packageCategory;

import com.medco.HealthConnectProvider.entity.packageCategory.PackageCategory;
import com.medco.HealthConnectProvider.entity.payers.Payer;
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
public interface PackageCategoryRepository extends JpaRepository<PackageCategory, Long>, JpaSpecificationExecutor<PackageCategory> {

    Optional<PackageCategory> findByCategoryUuid(String categoryUuid);

    Optional<PackageCategory> findByCategoryCodeAndPayer(String categoryCode, Payer payer);

    List<PackageCategory> findByPayerAndStatusOrderByCategoryNameAsc(Payer payer, Status status);

    Page<PackageCategory> findByPayerAndStatusOrderByCategoryNameAsc(Payer payer, Status status, Pageable pageable);

//    @Query("SELECT pc FROM PackageCategory pc WHERE pc.payer = :payer " +
//           "AND (:searchKey IS NULL OR :searchKey = '' OR " +
//           "LOWER(pc.categoryName) LIKE LOWER(CONCAT('%', :searchKey, '%')) OR " +
//           "LOWER(pc.categoryCode) LIKE LOWER(CONCAT('%', :searchKey, '%'))) " +
//           "AND pc.status = :status " +
//           "ORDER BY pc.categoryName ASC")
//    Page<PackageCategory> searchByPayerAndStatus(@Param("payer") Payer payer,
//                                                @Param("searchKey") String searchKey,
//                                                @Param("status") Status status,
//                                                Pageable pageable);

    boolean existsByCategoryCodeAndPayerAndIsDeletedFalse(String categoryCode, Payer payer);

    boolean existsByCategoryNameAndPayerAndIsDeletedFalse(String categoryName, Payer payer);

    long countByPayerAndStatus(Payer payer, Status status);

    @Query("SELECT pc FROM PackageCategory pc " +
           "JOIN pc.serviceCategoryMappings scm " +
           "JOIN scm.contractDetail cd " +
           "WHERE cd.contractDetailUuid = :contractDetailUuid " +
           "AND pc.status = 'ACTIVE'")
    List<PackageCategory> findCategoriesByContractDetailUuid(@Param("contractDetailUuid") String contractDetailUuid);
}
