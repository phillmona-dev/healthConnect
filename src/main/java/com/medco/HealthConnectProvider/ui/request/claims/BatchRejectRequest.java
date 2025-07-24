package com.medco.HealthConnectProvider.ui.request.claims;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchRejectRequest {

    @NotBlank(message = "Remark is required")
    @Size(max = 1000, message = "Remark must not exceed 1000 characters")
    private String remark;

}
