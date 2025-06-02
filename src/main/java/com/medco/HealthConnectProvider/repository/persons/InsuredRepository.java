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

import java.util.List;

@Repository
public interface InsuredRepository extends JpaRepository<Insured, Long> {

    Insured findByInsuredUuid(String insuredUuid);

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
     * Find insured persons by payer UUID and not deleted, with search capability
     * @param payerUuid The UUID of the payer
     * @param isDeleted Whether the insured person is deleted
     * @param firstName Search by first name
     * @param fatherName Search by father name
     * @param grandFatherName Search by grandfather name
     * @param phone Search by phone
     * @param insuranceId Search by insurance ID
     * @param pageable Pagination information
     * @return Page of insured persons
     */
    Page<Insured> findByPayerPayerUuidAndIsDeletedAndFirstNameContainingOrFatherNameContainingOrGrandFatherNameContainingOrPhoneContainingOrInsuranceIdContaining(
            String payerUuid, boolean isDeleted,
            String firstName, String fatherName, String grandFatherName,
            String phone, String insuranceId, Pageable pageable);

    /**
     * Find insured persons by payer UUID and not deleted, with search capability
     * @param payerUuid The UUID of the payer
     * @param isDeleted Whether the insured person is deleted
     * @param searchKey The search key for multiple fields
     * @param pageable Pagination information
     * @return Page of insured persons
     */
    @Query("SELECT DISTINCT i FROM Insured i " +
            "WHERE i.payer.payerUuid = :payerUuid " +
            "AND i.isDeleted = :isDeleted " +
            "AND (:searchKey IS NULL OR :searchKey = '' OR " +
            "    i.firstName LIKE %:searchKey% OR " +
            "    i.fatherName LIKE %:searchKey% OR " +
            "    i.grandFatherName LIKE %:searchKey% OR " +
            "    i.phone LIKE %:searchKey% OR " +
            "    i.insuranceId LIKE %:searchKey% OR " +
            "    CONCAT(i.firstName, ' ', i.fatherName) LIKE %:searchKey% OR " +
            "    CONCAT(i.firstName, ' ', i.fatherName, ' ', i.grandFatherName) LIKE %:searchKey%)")
    Page<Insured> findByPayerAndSearchKey(
            @Param("payerUuid") String payerUuid,
            @Param("isDeleted") boolean isDeleted,
            @Param("searchKey") String searchKey,
            Pageable pageable);

    /**
     * Count insured persons by payer UUID and not deleted, with search capability
     * @param payerUuid The UUID of the payer
     * @param isDeleted Whether the insured person is deleted
     * @param searchKey The search key for multiple fields
     * @return Count of insured persons
     */
    @Query("SELECT COUNT(DISTINCT i) FROM Insured i " +
            "WHERE i.payer.payerUuid = :payerUuid " +
            "AND i.isDeleted = :isDeleted " +
            "AND (:searchKey IS NULL OR :searchKey = '' OR " +
            "    i.firstName LIKE %:searchKey% OR " +
            "    i.fatherName LIKE %:searchKey% OR " +
            "    i.grandFatherName LIKE %:searchKey% OR " +
            "    i.phone LIKE %:searchKey% OR " +
            "    i.insuranceId LIKE %:searchKey% OR " +
            "    CONCAT(i.firstName, ' ', i.fatherName) LIKE %:searchKey% OR " +
            "    CONCAT(i.firstName, ' ', i.fatherName, ' ', i.grandFatherName) LIKE %:searchKey%)")
    long countByPayerAndSearchKey(
            @Param("payerUuid") String payerUuid,
            @Param("isDeleted") boolean isDeleted,
            @Param("searchKey") String searchKey);

    /**
     * Find eligibility information for an insured person by UUID
     * @param insuredUuid The UUID of the insured person
     * @return List of InsuredListResponse objects with eligibility information
     */
    @Query("SELECT new com.medco.HealthConnectProvider.ui.response.persons.InsuredListResponse(" +
            "i.insuredUuid, " +
            "ch.contractName, ch.contractCode, ch.startDate, ch.endDate, " +
            "ch.contractCode, ch.contractName, ch.startDate, ch.endDate, " +
            "p.payerName, p.telephone, pr.telephone, pr.providerName, " +
            "p.telephone, p.payerName, " +
            "i.email, i.title, i.firstName, i.fatherName, i.grandFatherName, i.gender, " +
            "i.birthDate, i.phone, i.branchOffice, i.position, i.idNumber, i.insuranceId, " +
            "i.profilePicture, 0.0, 0.0, " +
            "i.address1, i.address2, i.address3, i.state, i.country, " +
            "i.beginDate, i.endDate, CAST(i.status AS string)) " + // Changed to CAST as string
            "FROM Insured i " +
            "JOIN i.payer p " +
            "JOIN i.contracts ch " +
            "JOIN ch.provider pr " +
            "WHERE i.isDeleted = false AND i.insuredUuid = :insuredUuid")
    List<InsuredListResponse> findInsuredPersonEligibility(@Param("insuredUuid") String insuredUuid);


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

}