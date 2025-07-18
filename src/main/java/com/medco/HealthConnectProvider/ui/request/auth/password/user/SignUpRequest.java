package com.medco.HealthConnectProvider.ui.request.auth.password.user;

import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpRequest {

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    @NotBlank
    @Size(min = 2, max = 25)
    private String title;

    @NotBlank

    private String firstName;

    @NotBlank
    private String fatherName;

    private String grandFatherName;

    @NotBlank
    @Size(min = 1, max = 10)
    private String Gender;

    @NotBlank
    @Size(min = 9, max = 13)
    private String mobilePhone;

    private String roleUuid;

    private Status userStatus;

}
