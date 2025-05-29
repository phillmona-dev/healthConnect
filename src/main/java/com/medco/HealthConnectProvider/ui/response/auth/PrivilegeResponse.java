package com.medco.HealthConnectProvider.ui.response.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PrivilegeResponse implements Serializable {
    private String privilegeUuid;
    private String privilegeName;
    private String privilegeDescription;
    private String privilegeCategory;
}
