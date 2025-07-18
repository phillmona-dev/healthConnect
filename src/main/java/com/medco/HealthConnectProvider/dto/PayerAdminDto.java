package com.medco.HealthConnectProvider.dto;

import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PayerAdminDto {

    @NotBlank(message = "can't be empty")
    @Size(max = 50)
    @Email
    private String email;

    @NotBlank(message = "can't be empty")
    @Size(min = 6, max = 40)
    private String password;

    @NotBlank(message = "can't be empty")
    @Size(min = 2, max = 25)
    private String title;

    @NotBlank(message = "can't be empty")
    private String firstName;

    @NotBlank(message = "can't be empty")
    private String fatherName;

    //  @NotBlank(message = "can't be empty")
    private String grandFatherName;

    @NotBlank(message = "can't be empty")
    @Size(min = 1, max = 10)
    private String Gender;

    @NotBlank(message = "can't be empty")
    @Size(min = 9, max = 13)
    private String mobilePhone;


    private Status userStatus;

    @Size(max = 25)
    private String userType;

    //  private UUID providerUuid;
//
    @NotBlank(message = "can't be empty")
    private String payerUuid;


    @NotNull(message = "you need to select a role for the user")
    private String roleUuid;
}
