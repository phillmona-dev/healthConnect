//package com.medco.HealthConnectProvider.repository.claims;
//
//import com.medco.HealthConnectProvider.entity.services.ProvidedService;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface ProvidedServiceRepository extends JpaRepository<ProvidedService, Long> {
//
//    Optional<ProvidedService> findByProvidedServiceUuid(String providedServiceUuid);
//
//    List<ProvidedService> findByClaimUuid(String claimUuid);
//
//    @Query("SELECT SUM(ps.totalPrice) FROM ProvidedService ps WHERE ps.claimUuid = :claimUuid")
//    Double sumTotalPriceByClaimUuid(@Param("claimUuid") String claimUuid);
//
//    @Query("SELECT COUNT(ps) FROM ProvidedService ps WHERE ps.claimUuid = :claimUuid")
//    Long countByClaimUuid(@Param("claimUuid") String claimUuid);
//
//    void deleteByClaimUuid(String claimUuid);
//}