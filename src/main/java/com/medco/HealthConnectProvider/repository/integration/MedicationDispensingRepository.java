//package com.medco.HealthConnectProvider.repository.integration;
//
//import com.medco.HealthConnectProvider.entity.integration.MedicationDispensing;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//@Repository
//public interface MedicationDispensingRepository extends JpaRepository<MedicationDispensing, Long> {
//
//    boolean existsByPharmacyTransactionId(String pharmacyTransactionId);
//
//    MedicationDispensing findByDispensingUuid(String dispensingUuid);
//
//    List<MedicationDispensing> findByDispensingUuidIn(String[] dispensingUuids);
//
//    Page<MedicationDispensing> findByProviderUuidAndClaimStatus(
//            String providerUuid, String claimStatus, Pageable pageable);
//
//    Page<MedicationDispensing> findByProviderUuidAndInsuredUuidAndClaimStatus(
//            String providerUuid, String insuredUuid, String claimStatus, Pageable pageable);
//
//    List<MedicationDispensing> findByClaimUuid(String claimUuid);
//}