package com.medco.HealthConnectProvider.repository.persons;

import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.utils.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
