package com.medco.HealthConnectProvider.config.conf;

import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractRequest;
import com.medco.HealthConnectProvider.ui.response.contracts.ContractResponse;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setFieldMatchingEnabled(true)
                .setSkipNullEnabled(true);

        // ContractHeader to ContractResponse mapping
        modelMapper.typeMap(ContractHeader.class, ContractResponse.class)
                .addMappings(mapper -> {
                    // Map audit fields
                    mapper.map(src -> src.getCreatedAt(), ContractResponse::setCreatedAt);
                    mapper.map(src -> src.getUpdatedAt(), ContractResponse::setUpdatedAt);

                    // Map dates
                    mapper.map(ContractHeader::getStartDate, ContractResponse::setStartDate);
                    mapper.map(ContractHeader::getEndDate, ContractResponse::setEndDate);
                });

        // ContractRequest to ContractHeader mapping
        modelMapper.typeMap(ContractRequest.class, ContractHeader.class)
                .addMappings(mapper -> {
                    mapper.skip(ContractHeader::setId);
                    mapper.skip(ContractHeader::setContractHeaderUuid);
                    mapper.skip(ContractHeader::setStatus);
                    mapper.skip(ContractHeader::setPayer);
                    mapper.skip(ContractHeader::setProvider);
                    mapper.skip(ContractHeader::setContractDetails);
                    mapper.skip(ContractHeader::setInsured);
                });

        return modelMapper;
    }

    private void configureContractMappings(ModelMapper modelMapper) {
        // ContractHeader to ContractResponse mapping
        modelMapper.typeMap(ContractHeader.class, ContractResponse.class)
                .addMappings(mapper -> {
                    // Map payer information
                    mapper.map(src -> src.getPayer().getId(), ContractResponse::setPayerUuid);
                    mapper.map(src -> src.getPayer() != null ? src.getPayer().getPayerName() : null,
                            ContractResponse::setPayerName);
                    mapper.map(src -> src.getPayer() != null ? src.getPayer().getPayerCode() : null,
                            ContractResponse::setPayerCode);

                    // Map provider information
                    mapper.map(src -> src.getProvider().getId(), ContractResponse::setProviderUuid);
                    mapper.map(src -> src.getProvider() != null ? src.getProvider().getProviderName() : null,
                            ContractResponse::setProviderName);
                    mapper.map(src -> src.getProvider() != null ? src.getProvider().getProviderCode() : null,
                            ContractResponse::setProviderCode);

                    // Map status enum
                    mapper.map(ContractHeader::getStatus, ContractResponse::setStatus);

                    // Map dates
                    mapper.map(ContractHeader::getStartDate, ContractResponse::setStartDate);
                    mapper.map(ContractHeader::getEndDate, ContractResponse::setEndDate);
                });

        // ContractRequest to ContractHeader mapping (for create/update operations)
        modelMapper.typeMap(ContractRequest.class, ContractHeader.class)
                .addMappings(mapper -> {
                    mapper.skip(ContractHeader::setId);
                    mapper.skip(ContractHeader::setContractHeaderUuid);
                    mapper.skip(ContractHeader::setStatus);
                    mapper.skip(ContractHeader::setPreparedBy);
                    mapper.skip(ContractHeader::setContractDetails);
                    mapper.skip(ContractHeader::setInsured);
                });
    }
}
