package com.medco.HealthConnectProvider.ui.response.claims;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimLogResponse {
    private String logUuid;
    private String actionByUuid;
    private String actionByName;
    private String actionByRole;
    private Date actionDate;
    private String actionStatus;
    private String previousStatus;
    private String comment;
}