package com.medco.HealthConnectProvider.services.contract;

import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractDetailRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractRenewalRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractTerminationRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.group.ContractServiceGroupAssignmentRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.group.EmployeeGroupRequest;
import com.medco.HealthConnectProvider.ui.response.contracts.ContractDetailResponse;
import com.medco.HealthConnectProvider.ui.response.contracts.ContractListPayerResponse;
import com.medco.HealthConnectProvider.ui.response.contracts.ContractResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ContractService {
    ResponseEntity<?> createContract(@Valid ContractRequest contractRequest);

    ResponseEntity<?> updateContract(String contractUuid, @Valid ContractRequest contractRequest);

    ContractResponse getContract(String contractUuid);

    ResponseEntity<?> approveContract(String payerProviderContractUuid);

    ResponseEntity<?> payerAgreementResponse(String payerProviderContractUuid, String status, String remark);

    ResponseEntity<?> deleteContract(String contractUuid);

    List<ContractListPayerResponse> getPayerProvidersContractLists(String searchKey, Pageable pageable, String status);

    List<ContractListPayerResponse> getProvidersContractLists(String providerUuid, String searchKey, int page, int limit, Status status);

    //Filmon

    ResponseEntity<?> getAvailableProvidersForContract(String searchKey, Pageable pageable);
    ResponseEntity<?> getAvailableServicesForProvider(String providerUuid, String searchKey, Pageable pageable);
    ResponseEntity<?> addEmployeeGroupsToContract(String contractUuid, List<EmployeeGroupRequest> groups);
    ResponseEntity<?> assignServicesToEmployeeGroups(String contractUuid, List<ContractServiceGroupAssignmentRequest> assignments);


    ResponseEntity<?> addServiceToContract(String contractUuid, @Valid ContractDetailRequest detailRequest);
    ResponseEntity<?> updateContractDetail(String contractDetailUuid, @Valid ContractDetailRequest detailRequest);
    ResponseEntity<?> removeServiceFromContract(String contractDetailUuid);
    List<ContractDetailResponse> getContractDetails(String contractUuid, Pageable pageable);

    // Contract approval workflow
    ResponseEntity<?> submitContractForApproval(String contractUuid);
    ResponseEntity<?> reviewContract(String contractUuid, String reviewerComments, boolean approved);

    // Contract renewal
    ResponseEntity<?> initiateContractRenewal(String contractUuid, @Valid ContractRenewalRequest renewalRequest);
    ResponseEntity<?> cancelRenewal(String renewalUuid);

    // Contract termination
    ResponseEntity<?> terminateContract(String contractUuid, @Valid ContractTerminationRequest terminationRequest);
    ResponseEntity<?> withdrawTermination(String contractUuid);
}
