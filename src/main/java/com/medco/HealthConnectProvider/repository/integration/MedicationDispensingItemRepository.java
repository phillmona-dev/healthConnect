//package com.medco.HealthConnectProvider.repository.integration;
//
//import com.medco.HealthConnectProvider.entity.integration.MedicationDispensing;
//import com.medco.HealthConnectProvider.entity.integration.MedicationDispensingItem;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//@Repository
//public interface MedicationDispensingItemRepository extends JpaRepository<MedicationDispensingItem, Long> {
//
//    List<MedicationDispensingItem> findByDispensing(MedicationDispensing dispensing);
//
//    MedicationDispensingItem findByItemUuid(String itemUuid);
//}