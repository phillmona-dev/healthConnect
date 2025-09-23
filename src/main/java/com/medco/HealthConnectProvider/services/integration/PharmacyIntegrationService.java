package com.medco.HealthConnectProvider.services.integration;

import com.medco.HealthConnectProvider.dto.MedicationDispensingDTO;
import com.medco.HealthConnectProvider.dto.PendingDispensingRecordDTO;
import com.medco.HealthConnectProvider.ui.request.drug.DrugDispensingRecordEditRequest;
import com.medco.HealthConnectProvider.ui.request.drug.DrugDispensingRecordRequest;
import com.medco.HealthConnectProvider.ui.request.integration.DispensingRecordEditRequest;
import com.medco.HealthConnectProvider.ui.request.integration.DispensingRecordRequest;
import com.medco.HealthConnectProvider.ui.request.integration.CreateCbhiInsuredRequest;
import com.medco.HealthConnectProvider.ui.request.integration.CreateBulkCbhiInsuredRequest;
import com.medco.HealthConnectProvider.ui.request.integration.KenemaPharmacyDispensingRequest;
import com.medco.HealthConnectProvider.ui.request.integration.MedicationDispensingRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PayerResponse;
import com.medco.HealthConnectProvider.ui.response.integration.BulkCbhiInsuredResponse;
import com.medco.HealthConnectProvider.ui.response.claims.ReconciliationResponse;
import com.medco.HealthConnectProvider.ui.response.integration.DispensingDetailResponse;
import com.medco.HealthConnectProvider.ui.response.integration.DispensingResponse;

import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface PharmacyIntegrationService {

    /**
     * Creates a new claim from selected dispensing records
     *
     * @param providerUuid The UUID of the pharmacy provider
     * @param dispensingUuids Array of dispensing record UUIDs to include in the claim
     * @return Response with the created claim details
     */
    ResponseEntity<?> createClaimFromDispensingRecords(String providerUuid, String[] dispensingUuids);

    ResponseEntity<?> authorizeDispensingRecord(String dispensingUuid);

    ResponseEntity<?> authorizeDispensingRecords(String[] dispensingUuids);

    ResponseEntity<?> createClaimFromAuthorizedRecord(String providerUuid, String dispensingUuid);

    ResponseEntity<?> createClaimFromAuthorizedRecords(String providerUuid, String[] dispensingUuids);

    ResponseEntity<ReconciliationResponse> reconcilePayment(String claimUuid);

    
    ResponseEntity<?> updateDispensingRecordsStatus(String providerUuid, String newStatus, String[] dispensingUuids);

    ResponseEntity<?> addDispensingRecord(DispensingRecordRequest request, MultipartFile attachment);

    ResponseEntity<DispensingResponse> recordMedicationDispensing(@Valid KenemaPharmacyDispensingRequest request);

    ResponseEntity<?> addDrugDispensingRecord(DrugDispensingRecordRequest request);

    ResponseEntity<DispensingDetailResponse> getDispensingDetail(String dispensingUuid);

    ResponseEntity<?> editDispensingRecord(String dispensingUuid, @Valid DispensingRecordEditRequest editRequest);

    ResponseEntity<?> editDrugDispensingRecord(String dispensingUuid, @Valid DrugDispensingRecordEditRequest editRequest);

    ResponseEntity<?> updateServiceClaimStatus(String medicationDispensingUuid, String newStatus ,String remark);

    ResponseEntity<PagedResponse<MedicationDispensingDTO>> getMedicationsByBatchCode(String batchCode, int page, int size);

    ResponseEntity<PagedResponse<PendingDispensingRecordDTO>> getDispensingRecords(String providerUuid, String search, String status, LocalDate startDate,
                                                                                   LocalDate endDate, String payerUuid, int page, int size, String sortBy, String sortDirection);

    ResponseEntity<?> removeDispensingFromBatch(String dispensingUuid);

    ResponseEntity<?> createCbhiInsured(CreateCbhiInsuredRequest request);

    ResponseEntity<BulkCbhiInsuredResponse> createBulkCbhiInsured(CreateBulkCbhiInsuredRequest request);

    ResponseEntity<PagedResponse<PayerResponse>> getPayersForIntegration(
            String searchKey,
            Status status,
            String category,
            String payerName,
            Long tinNumber,
            Boolean isInsurance,
            Boolean isCbhi,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

//    ResponseEntity<?> createBatchClaim(String batchCode);
}