package com.medco.HealthConnectProvider.ui.request.packageCategory;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageCategoryLimitRequest {

    @NotBlank(message = "Category UUID is required")
    private String categoryUuid;

    @NotNull(message = "Limit type is required")
    @Pattern(regexp = "^(AMOUNT|QUANTITY|VISITS)$", message = "Limit type must be AMOUNT, QUANTITY, or VISITS")
    private String limitType;

    @NotNull(message = "Limit value is required")
    @DecimalMin(value = "0.01", message = "Limit value must be greater than 0")
    @Digits(integer = 17, fraction = 2, message = "Limit value must have at most 17 integer digits and 2 decimal places")
    private BigDecimal limitValue;

    @NotNull(message = "Period type is required")
    @Pattern(regexp = "^(ANNUAL|MONTHLY|PER_CLAIM|CONTRACT_PERIOD)$", 
             message = "Period type must be ANNUAL, MONTHLY, PER_CLAIM, or CONTRACT_PERIOD")
    private String periodType;

    @NotNull(message = "Reset date is required")
    @Future(message = "Reset date must be in the future")
    private LocalDate resetDate;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
}
