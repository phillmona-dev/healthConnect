package com.medco.HealthConnectProvider.ui.response.claims;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimAttachmentResponse {
    private String attachmentUuid;
    private String fileName;
    private String fileType;
    private String fileUrl;
    private Long fileSize;
    private Date uploadDate;
    private String uploadedByUuid;
    private String uploadedByName;
}