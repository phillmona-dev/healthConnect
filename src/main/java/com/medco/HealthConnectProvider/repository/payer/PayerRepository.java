package com.medco.HealthConnectProvider.repository.payer;

import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.utils.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayerRepository extends JpaRepository<Payer, Long> {

    boolean existsByTelephone(String telephone);

    boolean existsByEmail(String email);

    boolean existsByPayerName(String name);

    Payer findByPayerUuid(String institutionUuid);

    List<Payer> findAllByPayerName(String name);

    Page<Payer> findAllByIsDeletedAndStatus(boolean b, Status status, Pageable pageRequest);

    Page<Payer> findAllByIsDeletedAndPayerNameContainingAndStatus(boolean b, String search, Status status,
                                                                              Pageable pageRequest);

    @Query(value = " SELECT payers.* from payers INNER JOIN  payer_institution_contracts ON payers.payer_uuid=payer_institution_contracts.payer_uuid  WHERE payer_institution_contracts.is_deleted=0 and payer_institution_contracts.quotation_uuid=:quotationUuid  GROUP by payer_institution_contracts.quotation_uuid", nativeQuery = true)
    Payer findInstitutionByQuotationUuid(String quotationUuid);

    @Query(value = "  SELECT  MAX(payers.counter) as counter from payers", nativeQuery = true)
    Integer findMaxCounter();
}
