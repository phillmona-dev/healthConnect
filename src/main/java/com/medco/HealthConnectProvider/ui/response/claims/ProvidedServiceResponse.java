package com.medco.HealthConnectProvider.ui.response.claims;

import com.medco.HealthConnectProvider.dto.MedicationDispensingDTO;
import com.medco.HealthConnectProvider.ui.request.integration.MedicationDispensingRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProvidedServiceResponse {
    // Insured person information
    private String insuredPersonUuid;
    private String insuredPersonName;
    private String insuredPersonCode;
    private String policyNumber;
    private String insuredPersonPhone;
    private String insuredPersonGender;

    // Dependant information (if applicable)
    private String dependantUuid;
    private String dependantFullName;
    private String dependantRelationship;

    private LocalDate dispensingDate;
    private Double totalAmount;
//    List<ItemResponse> itemResponses;
    private List<MedicationDispensingDTO.MedicationItemDTO> medicationItems;

    private String invoiceNumber;
    private String prescriptionNumber;
    private String pharmacyTransactionId;

    private String prescribingPhysicianName;
    private String branchName;
    private String claimStatus;

    private String pharmacistNotes;
}