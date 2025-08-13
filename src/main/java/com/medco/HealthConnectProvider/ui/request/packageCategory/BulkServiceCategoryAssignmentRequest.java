package com.medco.HealthConnectProvider.ui.request.packageCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkServiceCategoryAssignmentRequest {

    @NotEmpty(message = "Contract detail UUIDs cannot be empty")
    private List<String> contractDetailUuids;

    @NotBlank(message = "Category UUID is required")
    private String categoryUuid;

    @NotNull(message = "Consumes from limit flag is required")
    private Boolean consumesFromLimit;

    private String notes;

    @Builder.Default
    private Boolean replaceExisting = false;
}
