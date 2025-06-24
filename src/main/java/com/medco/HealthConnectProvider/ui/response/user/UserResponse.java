package com.medco.HealthConnectProvider.ui.response.user;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse implements Serializable {

    private String userUuid;
    private String email;
    private String title;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String Gender;

    private String mobilePhone;
    private Status userStatus;
    private String payerUuid;
    private String providerUuid;
    private String profilePicture;
    private boolean isDeleted;
    private String roleName;

}
