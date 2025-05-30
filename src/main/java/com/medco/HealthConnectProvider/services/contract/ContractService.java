package com.medco.HealthConnectProvider.services.contract;

import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractRequest;
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
}
