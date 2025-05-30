package com.medco.HealthConnectProvider.ui.response.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RoleResponse implements Serializable{
    private String roleUuid;
    private String roleName;
    private String roleDescription;
    private List<PrivilegeResponse> privilegeList;
}
