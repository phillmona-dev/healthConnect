package com.medco.HealthConnectProvider.services.impl.service;

import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.services.ProvidedService;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.repository.claims.ClaimRepository;
import com.medco.HealthConnectProvider.repository.claims.ProvidedServiceRepository;
import com.medco.HealthConnectProvider.repository.contract.ContractDetailRepository;
import com.medco.HealthConnectProvider.services.service.ProvidedServiceService;
import com.medco.HealthConnectProvider.ui.request.service.ProvidedServiceRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.service.ProvidedServiceResponse;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProvidedServiceServiceImpl implements ProvidedServiceService {

    @Autowired
    private ProvidedServiceRepository providedServiceRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ContractDetailRepository contractDetailRepository;

    @Override
    public ResponseEntity<?> addProvidedService(String claimUuid, @Valid ProvidedServiceRequest providedServiceRequest) {
        // Verify claim exists
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new RuntimeException("Claim not found with UUID: " + claimUuid));

        // Verify contract detail exists
        ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(providedServiceRequest.getContractDetailUuid());
        if (contractDetail==null){
            throw new RuntimeException("Contract detail not found with UUID: " + providedServiceRequest.getContractDetailUuid());
        }

        // Create and save provided service
        ProvidedService providedService = new ProvidedService();
        providedService.setClaimUuid(claimUuid);
        providedService.setContractDetail(contractDetail);
        providedService.setQuantity(providedServiceRequest.getQuantity());
        providedService.setUnitPrice(providedServiceRequest.getUnitPrice());
        providedService.setTotalPrice(providedServiceRequest.getQuantity() * providedServiceRequest.getUnitPrice());

        providedServiceRepository.save(providedService);

        return ResponseEntity.ok(new MessageResponse("Provided service added successfully"));
    }

    @Override
    public ResponseEntity<?> updateProvidedService(String providedServiceUuid, @Valid ProvidedServiceRequest providedServiceRequest) {
        // Find provided service
        ProvidedService providedService = providedServiceRepository.findByProvidedServiceUuid(providedServiceUuid)
                .orElseThrow(() -> new RuntimeException("Provided service not found with UUID: " + providedServiceUuid));

        // Verify contract detail exists if it's being updated
        if (providedServiceRequest.getContractDetailUuid() != null) {
            ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(providedServiceRequest.getContractDetailUuid());
            if (contractDetail==null){
                throw new RuntimeException("Contract detail not found with UUID: " + providedServiceRequest.getContractDetailUuid());
            }

            providedService.setContractDetail(contractDetail);
        }

        // Update fields
        if (providedServiceRequest.getQuantity() != null) {
            providedService.setQuantity(providedServiceRequest.getQuantity());
        }
        
        if (providedServiceRequest.getUnitPrice() != null) {
            providedService.setUnitPrice(providedServiceRequest.getUnitPrice());
        }
        
        // Recalculate total price
        providedService.setTotalPrice(providedService.getQuantity() * providedService.getUnitPrice());
        
        providedServiceRepository.save(providedService);
        
        return ResponseEntity.ok(new MessageResponse("Provided service updated successfully"));
    }

    @Override
    public ProvidedServiceResponse getProvidedService(String providedServiceUuid) {
        ProvidedService providedService = providedServiceRepository.findByProvidedServiceUuid(providedServiceUuid)
                .orElseThrow(() -> new RuntimeException("Provided service not found with UUID: " + providedServiceUuid));
        
        return mapToResponse(providedService);
    }

    @Override
    public List<ProvidedServiceResponse> getProvidedServicesByClaim(String claimUuid) {
        // Verify claim exists
        if (!claimRepository.findByClaimUuid(claimUuid).isPresent()) {
            throw new RuntimeException("Claim not found with UUID: " + claimUuid);
        }
        
        List<ProvidedService> providedServices = providedServiceRepository.findByClaimUuid(claimUuid);
        return providedServices.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteProvidedService(String providedServiceUuid) {
        ProvidedService providedService = providedServiceRepository.findByProvidedServiceUuid(providedServiceUuid)
                .orElseThrow(() -> new RuntimeException("Provided service not found with UUID: " + providedServiceUuid));
        
        providedServiceRepository.delete(providedService);
        
        return ResponseEntity.ok(new MessageResponse("Provided service deleted successfully"));
    }

    @Override
    public Double calculateTotalPriceForClaim(String claimUuid) {
        // Verify claim exists
        if (!claimRepository.findByClaimUuid(claimUuid).isPresent()) {
            throw new RuntimeException("Claim not found with UUID: " + claimUuid);
        }
        
        Double total = providedServiceRepository.sumTotalPriceByClaimUuid(claimUuid);
        return total != null ? total : 0.0;
    }

    @Override
    public Long countProvidedServicesForClaim(String claimUuid) {
        // Verify claim exists
        if (!claimRepository.findByClaimUuid(claimUuid).isPresent()) {
            throw new RuntimeException("Claim not found with UUID: " + claimUuid);
        }
        
        return providedServiceRepository.countByClaimUuid(claimUuid);
    }

    @Override
    @Transactional
    public ResponseEntity<?> addMultipleProvidedServices(String claimUuid, List<ProvidedServiceRequest> providedServiceRequests) {
        // Verify claim exists
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new RuntimeException("Claim not found with UUID: " + claimUuid));
        
        if (providedServiceRequests == null || providedServiceRequests.isEmpty()) {
            throw new BadRequestException("No provided services to add");
        }
        
        List<ProvidedService> servicesToSave = new ArrayList<>();
        
        for (ProvidedServiceRequest request : providedServiceRequests) {
            // Verify contract detail exists
            ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(request.getContractDetailUuid());
            if (contractDetail==null){
                throw new RuntimeException("Contract detail not found with UUID: " + request.getContractDetailUuid());
            }
            
            ProvidedService providedService = new ProvidedService();
            providedService.setClaimUuid(claimUuid);
            providedService.setContractDetail(contractDetail);
            providedService.setQuantity(request.getQuantity());
            providedService.setUnitPrice(request.getUnitPrice());
            providedService.setTotalPrice(request.getQuantity() * request.getUnitPrice());
            
            servicesToSave.add(providedService);
        }
        
        providedServiceRepository.saveAll(servicesToSave);
        
        return ResponseEntity.ok(new MessageResponse(servicesToSave.size() + " provided services added successfully"));
    }

    private ProvidedServiceResponse mapToResponse(ProvidedService providedService) {
        ProvidedServiceResponse response = new ProvidedServiceResponse();
        BeanUtils.copyProperties(providedService, response);

        // Add service details from contract detail
        response.setServiceName(providedService.getContractDetail().getServicelist().getServiceName());
        response.setServiceCode(providedService.getContractDetail().getServicelist().getServiceCode());
        response.setContractDetailUuid(providedService.getContractDetail().getContractDetailUuid());

        return response;
    }
}