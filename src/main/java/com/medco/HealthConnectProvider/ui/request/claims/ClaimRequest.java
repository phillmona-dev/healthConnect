package com.medco.HealthConnectProvider.ui.request.claims;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimRequest {
    
    @NotBlank(message = "Contract UUID is required")
    private String contractUuid;
    
    @NotBlank(message = "Insured person UUID is required")
    private String insuredPersonUuid;
    
    private String dependantUuid;
    
    @NotBlank(message = "MRN number is required")
    @Size(min = 5, max = 50, message = "MRN number must be between 5 and 50 characters")
    private String mrnNumber;
    
    @NotNull(message = "Visit date is required")
    private Date visitDate;
    
    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private Double totalAmount;
    
    @Size(max = 500, message = "Provider comment cannot exceed 500 characters")
    private String providerComment;
    
    // List of provided services included in this claim
    private List<String> providedServiceUuids;
}