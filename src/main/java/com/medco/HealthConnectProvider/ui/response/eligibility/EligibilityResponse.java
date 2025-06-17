package com.medco.HealthConnectProvider.ui.response.eligibility;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityResponse {
    // Insured person details
    private String insuredUuid;
    private String employeeId;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String insuranceId;
    private String nationalId;
    private String phoneNumber;
    private Status status;
    private Date birthDate;
    
    // Payer details
    private String payerUuid;
    private String payerName;
    
    // Policy details
    private String policyNumber;
    private LocalDate policyStartDate;
    private LocalDate policyEndDate;
    private boolean isPolicyActive;
    
    // Group memberships
    private List<GroupMembershipResponse> groups;
    
    // Dependents
    private List<DependentEligibilityResponse> dependents;
    
    // Specific service eligibility (if requested)
    private ServiceEligibilityResponse requestedService;
    
    // Overall eligibility status
    private boolean isEligible;
    private String ineligibilityReason;

    private String profilePictureBase64;

    public boolean hasRequestedService(){
        return requestedService != null;
    }
}