package com.medco.HealthConnectProvider.ui.request.packageCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCategoryMappingRequest {

    @NotBlank(message = "Contract detail UUID is required")
    private String contractDetailUuid;

    @NotEmpty(message = "At least one category UUID is required")
    private List<String> categoryUuids;

    private boolean consumesFromLimit = true;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
