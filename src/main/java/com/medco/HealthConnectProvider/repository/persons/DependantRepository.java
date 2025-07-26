package com.medco.HealthConnectProvider.repository.persons;

import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.utils.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DependantRepository extends JpaRepository<Dependant, Long> {

    Dependant findByDependantUuid(String dependantUuid);

//	List<Dependant> findAllByIsDeletedAndInsuredPersonUuid(boolean b, String insuredPersonUuid);



//	List<Dependant> findAllByIsDeletedAndInsuredPersonUuidAndStatus(boolean b, String insuredPersonUuid, Status status);
//
//	Dependant findByDependantUuidAndInsuredPersonUuidAndStatusAndIsDeleted(String dependantUuid,
//			String insuredPersonUuid, Status active, boolean b);




    List<Dependant> findAllByIsDeletedAndInsuredInsuredUuidAndStatus(boolean b, String insuredPersonUuid, Status status);


    Dependant findByDependantUuidAndInsuredInsuredUuidAndStatusAndIsDeleted(String dependantUuid, String insuredPersonUuid, Status status, boolean b);

    

    List<Dependant> findByInsuredAndIsDeletedFalse(Insured insured);

    List<Dependant> findByInsuredInsuredUuid(String insuredUuid);

    Page<Dependant> findByInsured(Insured insured, Pageable pageable);

    List<Dependant> findByInsured(Insured insured);

    @Override
    @Query("SELECT d FROM Dependant d WHERE d.isDeleted = false")
    List<Dependant> findAll();

    @Query("SELECT d FROM Dependant d WHERE d.insured.insuredUuid = :insuredUuid AND d.isDeleted = false")
    List<Dependant> findByInsuredUuid(String insuredUuid);

    List<Dependant> findByDependantUuidIn(List<String> memberUuids);

    Dependant findByPhone(String patientId);

    Long countByInsuredPayerPayerUuidAndIsDeleted(String payerUuid, boolean b);
}
