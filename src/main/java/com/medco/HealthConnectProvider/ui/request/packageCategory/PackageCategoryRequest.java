package com.medco.HealthConnectProvider.ui.request.packageCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    private String categoryName;

    @NotBlank(message = "Category code is required")
    @Size(max = 10, message = "Category code must not exceed 10 characters")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Category code must contain only uppercase letters, numbers, and underscores")
    private String categoryCode;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Status must be either ACTIVE or INACTIVE")
    private String status = "ACTIVE";
}
