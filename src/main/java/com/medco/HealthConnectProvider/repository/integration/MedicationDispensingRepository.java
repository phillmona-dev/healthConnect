package com.medco.HealthConnectProvider.repository.integration;

import com.medco.HealthConnectProvider.entity.claims.BatchRecord;
import com.medco.HealthConnectProvider.entity.integration.MedicationDispensing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicationDispensingRepository extends JpaRepository<MedicationDispensing, Long>, JpaSpecificationExecutor<MedicationDispensing> {

    boolean existsByPharmacyTransactionId(String pharmacyTransactionId);

    MedicationDispensing findByDispensingUuid(String dispensingUuid);

    List<MedicationDispensing> findByDispensingUuidIn(String[] dispensingUuids);

    List<MedicationDispensing> findByClaimUuid(String claimUuid);


    @Modifying
    @Query("UPDATE MedicationDispensing m SET m.claimStatus = :claimStatus, m.claimUuid = :claimUuid WHERE m.dispensingUuid = :dispensingUuid")
    int updateClaimStatus(@Param("dispensingUuid") String dispensingUuid,
                          @Param("claimStatus") String claimStatus,
                          @Param("claimUuid") String claimUuid);


    Page<MedicationDispensing> findByBatchCode(String batchCode, Pageable pageable);

}