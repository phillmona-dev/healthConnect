package com.medco.HealthConnectProvider.ui.request.claims;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimAttachmentRequest {
    
    @NotBlank(message = "Attachment type is required")
    @Size(max = 50, message = "Attachment type cannot exceed 50 characters")
    private String attachmentType; // RECEIPT, PRESCRIPTION, LAB_RESULT, MEDICAL_REPORT, OTHER
    
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
}