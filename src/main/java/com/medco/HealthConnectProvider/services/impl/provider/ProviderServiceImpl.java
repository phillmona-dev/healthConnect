package com.medco.HealthConnectProvider.services.impl.provider;

import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.repository.contract.ContractRepository;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
import com.medco.HealthConnectProvider.services.providers.ProviderService;
import com.medco.HealthConnectProvider.ui.request.auth.password.providers.ProviderRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.provider.PayersNameForProviderResponse;
import com.medco.HealthConnectProvider.ui.response.providers.ProviderResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProviderServiceImpl implements ProviderService {

    @Autowired
    ProviderRepository providerRepository;

    @Autowired
    ContractRepository contractRepository;

    @Override
    public ResponseEntity<ProviderResponse> createProvider(ProviderRequest providerRequest) {
        if (providerRepository.existsByEmail(providerRequest.getEmail())){
            ProviderResponse response = new ProviderResponse();
            response.setStatus("Error: Email is already in use!");
            return ResponseEntity.badRequest().body(response);
        }

        if (providerRepository.existsByProviderName(providerRequest.getProviderName())){
            ProviderResponse response = new ProviderResponse();
            response.setStatus("Error: Provider name is already in use!");
            return ResponseEntity.badRequest().body(response);
        }

        if (providerRepository.existsByTelephone(providerRequest.getTelephone())){
            ProviderResponse response = new ProviderResponse();
            response.setStatus("Error: phone number is already in use!");
            return ResponseEntity.badRequest().body(response);
        }

        Provider provider = new Provider();
        BeanUtils.copyProperties(providerRequest, provider);
        Provider savedProvider = providerRepository.save(provider);

        ProviderResponse providerResponse = new ProviderResponse();
        BeanUtils.copyProperties(savedProvider, providerResponse);
        providerResponse.setStatus("Provider added successfully");
        providerResponse.setProviderUuid(savedProvider.getProviderUuid());

        return ResponseEntity.ok(providerResponse);
    }

    @Override
    public ResponseEntity<?> updateProvider(String providerUuid, ProviderRequest providerRequest) {
        Optional<Provider> provider = Optional.ofNullable(providerRepository.findByProviderUuid(providerUuid)
                .orElseThrow(() -> new BadRequestException("Can't find Hospital With the provided Id")));

        BeanUtils.copyProperties(providerRequest, provider.get());
        providerRepository.save(provider.get());
        return ResponseEntity.ok(new MessageResponse("Provider Updated Successfully!"));
    }

    @Override
    public ProviderResponse getProvider(String providerUuid) {
        Provider provider = providerRepository.findByProviderUuid(providerUuid)
                .orElseThrow(()->new RuntimeException("provider not found with UUID:" + providerUuid));

        ProviderResponse providerResponse = new ProviderResponse();
        BeanUtils.copyProperties(provider, providerResponse);
        return providerResponse;
    }

    @Override
    public List<ProviderResponse> getProviders(String searchKey, int page, int limit) {
        //UserPrincipal userDetails = AuthUtil.getCurrentUser();

        if (page > 0)
            page = page - 1;
        Pageable pageRequest = PageRequest.of(page, limit, Sort.by("id").descending());
        Page<Provider> provider;
        if (searchKey != null)
            provider = providerRepository.findAllByProviderNameContaining( searchKey, pageRequest);
        else provider = providerRepository.findAll( pageRequest);
        long totalPages = provider.getTotalPages();
        List<Provider> providerList = provider.getContent();

        List<ProviderResponse> providerResponse = new ArrayList<>();
        for (Provider p : providerList) {

            ProviderResponse pr = new ProviderResponse();
            if (providerResponse.size() == 0)
                pr.setTotalPages(totalPages);
            BeanUtils.copyProperties(p, pr);
            providerResponse.add(pr);
        }
        return providerResponse;
    }

    @Override
    public ResponseEntity<?> deleteProvider(String providerUuid) {
        Optional<Provider> provider = Optional.ofNullable(providerRepository.findByProviderUuid(providerUuid)
                .orElseThrow(() -> new BadRequestException("can't find Hospital with the provided Id")));

        provider.get().setDeleted(true);
        providerRepository.save(provider.get());
        return ResponseEntity.ok(new MessageResponse("Provider soft deleted successfully!"));
    }

    @Override
    public List<ProviderResponse> getAvailableProvidersForPayerNotInContract(String payerUuid, String searchKey, int page, int limit) {
        if (page > 0) {
            page = page - 1;
        }

        Pageable pageRequest = PageRequest.of(page, limit, Sort.by("id").descending());
        Page<Provider> providerPage;

        // Find providers not already in contract with this payer
        if (searchKey != null && !searchKey.isEmpty()) {

            providerPage = providerRepository.findAvailableProvidersForPayerWithSearch(payerUuid, searchKey, pageRequest);

        } else {
            providerPage = providerRepository.findAvailableProvidersForPayerNotInContract(payerUuid, pageRequest);
        }

        List<Provider> providerList = providerPage.getContent();
        List<ProviderResponse> providerResponses = new ArrayList<>();

        for (Provider provider : providerList) {
            ProviderResponse response = new ProviderResponse();
            BeanUtils.copyProperties(provider, response);

            // Add total pages to first response
            if (providerResponses.isEmpty()) {
                response.setTotalPages((int) providerPage.getTotalPages());
            }

            providerResponses.add(response);
        }

        return providerResponses;
    }

    @Override
    public List<PayersNameForProviderResponse> getPayersNameForProvider(String providerUuid, String searchKey) {
        List<ContractHeader> contractList = searchKey != null ? getPayerNameWithSearch(providerUuid,searchKey) : getPayers(providerUuid);

        return contractList.stream()
                .map(contract -> {
                    var response = new PayersNameForProviderResponse();
                    BeanUtils.copyProperties(contract, response);

                    return response;
                }).collect(Collectors.toList());
    }

    private List<ContractHeader> getPayers(String providerUuid) {
        return contractRepository.findAllByProviderProviderUuidAndIsDeletedGroupByPayerPayerUuid(providerUuid, false);
    }

    private List<ContractHeader> getPayerNameWithSearch(String providerUuid, String searchKey) {
        return contractRepository.findAllByProviderProviderUuidAndIsDeletedGroupByPayerPayerUuidAndContainingName(providerUuid, false, searchKey);
    }
}
