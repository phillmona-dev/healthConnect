package com.medco.HealthConnectProvider.ui.request.auth.password.persons;

import com.medco.HealthConnectProvider.utils.enums.Relationship;
import com.medco.HealthConnectProvider.utils.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Schema(description = "Request object for a dependant of an insured person")
@AllArgsConstructor
@NoArgsConstructor
public class DependantRequest {
    @NotBlank(message = "Insured person UUID is required")
    private String insuredPersonUuid;

    @NotBlank(message = "Dependant first name is required")
    private String dependantFirstName;

    @NotBlank(message = "Dependant father name is required")
    private String dependantFatherName;

    @NotBlank(message = "Dependant grand father name is required")
    private String dependantGrandFatherName;

    @NotBlank(message = "Dependant gender is required")
    private String dependantGender;

    @NotNull(message = "Dependant birth date is required")
    private Date dependantBirthDate;

    @NotNull(message = "Relationship is required")
    private Relationship relationship;

    private Status dependantStatus;

}
