package com.medco.HealthConnectProvider.repository.persons;

import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.ui.response.persons.InsuredDependantListResponse;
import com.medco.HealthConnectProvider.ui.response.persons.InsuredListResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InsuredRepository extends JpaRepository<Insured, Long> {

    @Query("SELECT i FROM Insured i WHERE i.insuredUuid = :insuredUuid AND i.isDeleted = false")
    Insured findByInsuredUuid(@Param("insuredUuid") String insuredUuid);

    boolean existsByEmailAndInsuranceIdAndPayerUuid(String stringCellValue, String stringCellValue2,
                                                    String payerUuid);

    boolean existsByPhoneAndInsuranceIdAndPayerUuid(String stringCellValue, String stringCellValue2,
                                                    String payerUuid);

    boolean existsByInsuranceIdAndPayerUuid(String stringCellValue, String payerUuid);

    Page<Insured> findAllByIsDeletedAndFirstNameContainingOrPhoneContaining(boolean b, String search, String search2, Pageable pageRequest);

    Page<Insured> findAllByIsDeleted(boolean b, Pageable pageRequest);

    boolean existsByIsDeleted(boolean b);

    boolean existsByEmailAndPayerPayerUuid(@Size(max = 50) String email, @Size(min = 36, max = 40) String payerUuid);

    boolean existsByPhoneAndPayerPayerUuid(@NotBlank @Size(min = 9, max = 13) String phone, @Size(min = 36, max = 40) String payerUuid);

    Page<Insured> findByPayerPayerUuid(String payerUuid, Pageable pageable);


    /**
     * Find insured persons and their dependants by payer contract UUID with pagination
     * @param contractHeaderUuid The UUID of the contract header
     * @return List of InsuredDependantListResponse objects
     */
    @Query("SELECT new com.medco.HealthConnectProvider.ui.response.persons.InsuredDependantListResponse(" +
            "i.insuredUuid, i.firstName, i.insuranceId, i.fatherName, i.email, i.phone, " +
            "i.grandFatherName, i.title, i.gender, " +
            "d.firstName, d.fatherName, d.grandFatherName, d.gender, " +
            "d.relationship, d.dependantUuid, d.status) " +
            "FROM Insured i " +
            "JOIN i.payer p " +
            "JOIN i.contracts c " +
            "LEFT JOIN i.dependants d " +
            "WHERE c.contractHeaderUuid = :contractHeaderUuid " +
            "AND i.isDeleted = false " +
            "AND (d IS NULL OR d.isDeleted = false) " +
            "ORDER BY i.firstName ASC, i.fatherName ASC, i.grandFatherName ASC, " +
            "d.firstName ASC, d.fatherName ASC, d.grandFatherName ASC")
    List<InsuredDependantListResponse> findInsuredPersonsAndDependants(
            @Param("contractHeaderUuid") String contractHeaderUuid,
            Pageable pageable);

    /**
     * Find an insured person by phone number and payer UUID
     * @param phone The phone number
     * @param payerUuid The UUID of the payer
     * @return The insured person or null if not found
     */
    @Query("SELECT i FROM Insured i WHERE i.phone = :phone AND i.payer.payerUuid = :payerUuid AND i.isDeleted = false")
    Insured findByPhoneAndPayer(@Param("phone") String phone, @Param("payerUuid") String payerUuid);

    /**
     * Find an insured person by insurance ID and payer UUID
     * @param insuranceId The insurance ID
     * @param payerUuid The UUID of the payer
     * @return The insured person or null if not found
     */
    @Query("SELECT i FROM Insured i WHERE i.insuranceId = :insuranceId AND i.payer.payerUuid = :payerUuid AND i.isDeleted = false")
    Insured findByInsuranceIdAndPayer(@Param("insuranceId") String insuranceId, @Param("payerUuid") String payerUuid);

    /**
     * Find an insured person by employee ID and payer UUID
     * @param employeeId The employee ID
     * @param payerUuid The UUID of the payer
     * @return The insured person or null if not found
     */
    @Query("SELECT i FROM Insured i WHERE i.employeeId = :employeeId AND i.payer.payerUuid = :payerUuid AND i.isDeleted = false")
    Insured findByEmployeeIdAndPayer(@Param("employeeId") String employeeId, @Param("payerUuid") String payerUuid);

    /**
     * Find an insured person by national ID and payer UUID
     * @param nationalId The national ID
     * @param payerUuid The UUID of the payer
     * @return The insured person or null if not found
     */
    @Query("SELECT i FROM Insured i WHERE i.nationalId = :nationalId AND i.payer.payerUuid = :payerUuid AND i.isDeleted = false")
    Insured findByNationalIdAndPayer(@Param("nationalId") String nationalId, @Param("payerUuid") String payerUuid);


    @Query("SELECT i FROM Insured i WHERE " +
            "LOWER(i.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(i.fatherName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(i.grandFatherName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(i.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(i.insuranceId) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Insured> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT i FROM Insured i WHERE i.payer.payerUuid = :payerUuid AND i.isDeleted = false")
    Page<Insured> findByPayerUuid(@Param("payerUuid") String payerUuid, Pageable pageable);


    Page<Insured> findByPayerPayerUuidAndIsDeleted(String payerUuid, boolean isDeleted, Pageable pageable);

    @Query("SELECT i FROM Insured i WHERE i.payer.payerUuid = :payerUuid AND i.isDeleted = :isDeleted " +
            "AND (LOWER(i.firstName) LIKE LOWER(CONCAT('%', :searchKey, '%')) " +
            "OR LOWER(i.fatherName) LIKE LOWER(CONCAT('%', :searchKey, '%')) " +
            "OR LOWER(i.grandFatherName) LIKE LOWER(CONCAT('%', :searchKey, '%')) " +
            "OR LOWER(i.phone) LIKE LOWER(CONCAT('%', :searchKey, '%')) " +
            "OR LOWER(i.email) LIKE LOWER(CONCAT('%', :searchKey, '%')) " +
            "OR LOWER(i.insuranceId) LIKE LOWER(CONCAT('%', :searchKey, '%')))")
    Page<Insured> findByPayerAndSearchKey(@Param("payerUuid") String payerUuid,
                                          @Param("isDeleted") boolean isDeleted,
                                          @Param("searchKey") String searchKey,
                                          Pageable pageable);


    Insured findByInsuranceId(String insuranceId);

    Insured findByEmployeeId(String employeeId);

    Insured findByNationalId(String nationalId);

    //Insured findByPhone(String phone);

    List<Insured> findByPhoneOrEmployeeIdOrNationalId(String phone, String employeeId, String nationalId);

//    Insured findByIdNumber(String identifier);

    List<Insured> findByIdNumber(String idNumber);

    Collection<? extends Insured> findByPhone(String phone);

    List<Insured> findByInsuredUuidIn(List<String> insuredUuids);

    boolean existsByPhoneAndPayerUuid(String phone, String payerUuid);

    @Query("SELECT i FROM Insured i " +
            "WHERE i.payer.payerUuid = :payerUuid " +
            "AND i.isDeleted = :isDeleted " +
            "AND i NOT IN (SELECT ci FROM ContractHeader ch JOIN ch.insured ci " +
            "              WHERE ch.contractHeaderUuid = :contractUuid)")
    Page<Insured> findByPayerAndNotInContract(
            @Param("payerUuid") String payerUuid,
            @Param("contractUuid") String contractUuid,
            @Param("isDeleted") boolean isDeleted,
            Pageable pageable
    );

    @Query("SELECT DISTINCT i FROM Insured i " +
            "LEFT JOIN i.contracts c " +
            "WHERE i.payer.payerUuid = :payerUuid " +
            "AND i.isDeleted = :isDeleted " +
            "AND (c.contractHeaderUuid != :contractUuid OR c IS NULL) " +
            "AND (:searchKey IS NULL OR :searchKey = '' OR " +
            "  LOWER(i.firstName) LIKE LOWER(CONCAT('%', :searchKey, '%')) " +
            "  OR LOWER(i.fatherName) LIKE LOWER(CONCAT('%', :searchKey, '%')) " +
            "  OR LOWER(i.grandFatherName) LIKE LOWER(CONCAT('%', :searchKey, '%')) " +
            "  OR LOWER(i.phone) LIKE LOWER(CONCAT('%', :searchKey, '%')) " +
            "  OR LOWER(i.insuranceId) LIKE LOWER(CONCAT('%', :searchKey, '%')))")
    Page<Insured> findByPayerAndNotInContractAndSearchKey(
            @Param("payerUuid") String payerUuid,
            @Param("contractUuid") String contractUuid,
            @Param("isDeleted") boolean isDeleted,
            @Param("searchKey") String searchKey,
            Pageable pageable);

    boolean existsByIdNumberAndPayerUuid(String idNumber, String payerUuid);

    boolean existsByEmailAndPayerUuid(String email, String payerUuid);

    Insured findByIdNumberAndFirstNameAndFatherNameAndPayerUuid(String idNumber, String firstName, String fatherName, String payerUuid);

}