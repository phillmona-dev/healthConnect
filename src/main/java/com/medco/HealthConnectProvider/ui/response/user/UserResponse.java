package com.medco.HealthConnectProvider.ui.response.user;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class UserResponse implements Serializable {

    private String userUuid;
    private String email;
    private String title;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String Gender;

    private String mobilePhone;
    private String userStatus;
    private String providerUuid;
    private String profilePicture;
    private boolean isDeleted;
}
