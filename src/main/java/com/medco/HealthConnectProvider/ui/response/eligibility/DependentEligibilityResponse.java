package com.medco.HealthConnectProvider.ui.response.eligibility;

import com.medco.HealthConnectProvider.utils.enums.Relationship;
import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DependentEligibilityResponse {
    private String dependantUuid;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private Date birthDate;
    private Relationship relationship;
    private Status status;
    private List<GroupMembershipResponse> groups;
    private boolean isEligible;

    private String profilePictureBase64;
}