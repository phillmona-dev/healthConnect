package com.medco.HealthConnectProvider.ui.request.claims;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimCommentRequest {
    
    @NotBlank(message = "Comment is required")
    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    private String comment;
    
    private String commentType; // PROCESSOR, CHECKER, APPROVER, AUTHORIZER
}