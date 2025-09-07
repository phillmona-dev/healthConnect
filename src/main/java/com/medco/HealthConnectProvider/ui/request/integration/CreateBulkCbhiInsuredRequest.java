package com.medco.HealthConnectProvider.ui.request.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateBulkCbhiInsuredRequest {

    @NotBlank(message = "Payer UUID is required")
    private String payerUuid;

    @NotNull(message = "Insured list cannot be null")
    @NotEmpty(message = "Insured list cannot be empty")
    @Valid
    private List<InsuredMemberData> insuredMembers;

    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InsuredMemberData {
        
        @NotBlank(message = "First name is required")
        private String firstName;
        
        @NotBlank(message = "Father name is required")
        private String fatherName;
        
        private String grandFatherName;
        
        private String phone;
        
        private String email;
        
        private String nationalId;
        
        private String idNumber;
        
        private String insuranceId;
        
        @NotNull(message = "Birth date is required")
        private LocalDate birthDate;
        
        @NotBlank(message = "Gender is required")
        private String gender;
        
        private String address;
        
        private String city;
        
        private String state;
        
        private String country;
    }
}
