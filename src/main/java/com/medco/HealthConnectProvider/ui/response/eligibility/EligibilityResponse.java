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

    private String insuredUuid;
    private String employeeId;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String insuranceId;
    private String nationalId;
    private String phoneNumber;
    private String idNumber;
    private Status status;
    private Date birthDate;

    private String payerUuid;
    private String payerName;

    private String policyNumber;
    private LocalDate policyStartDate;
    private LocalDate policyEndDate;
    private boolean isPolicyActive;

    private List<GroupMembershipResponse> groups;

    private List<DependentEligibilityResponse> dependents;

    private ServiceEligibilityResponse requestedService;

    private boolean isEligible;
    private String ineligibilityReason;

    private String profilePictureBase64;

    public boolean hasRequestedService(){
        return requestedService != null;
    }
}