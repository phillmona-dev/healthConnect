package com.medco.HealthConnectProvider.ui.request.auth.password;

import com.medco.HealthConnectProvider.utils.enums.PrivilegeType;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrivilegeRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String privilegeName;

    @NotBlank
    @Size(max = 100)
    private String privilegeDescription;

    @NotBlank
    @Column(length = 50)
    private String privilegeCategory;

    @NotNull(message = "privilege type can't be empty ")
    private PrivilegeType  privilegeType;

}
