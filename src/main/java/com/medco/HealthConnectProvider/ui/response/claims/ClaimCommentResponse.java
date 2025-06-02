package com.medco.HealthConnectProvider.ui.response.claims;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimCommentResponse {
    private String commentUuid;
    private String comment;
    private String commentType;
    private Date commentDate;
    private String commentByUuid;
    private String commentByName;
    private String commentByRole;
}