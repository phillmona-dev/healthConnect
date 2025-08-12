package com.medco.HealthConnectProvider.dto.packageCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkServiceMappingRequest {

    @NotBlank(message = "Contract UUID is required")
    private String contractUuid;

    @NotEmpty(message = "Service mappings cannot be empty")
    private List<ServiceMappingItem> serviceMappings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceMappingItem {
        
        @NotBlank(message = "Service code is required")
        private String serviceCode;
        
        @NotBlank(message = "Service name is required")
        private String serviceName;
        
        @NotEmpty(message = "Category codes cannot be empty")
        private List<String> categoryCodes;
        
        @NotNull(message = "Consumes from limit flag is required")
        private Boolean consumesFromLimit;
        
        private String notes;
    }
}
