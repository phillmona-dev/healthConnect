package com.medco.HealthConnectProvider.ui.response.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSummary {
    private String userUuid;
    private String email;
    private String title;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String gender;
    private String mobilePhone;
    private String roleName;
}
