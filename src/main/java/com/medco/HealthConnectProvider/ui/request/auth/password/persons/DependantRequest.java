package com.medco.HealthConnectProvider.ui.request.auth.password.persons;

import com.medco.HealthConnectProvider.utils.enums.Relationship;
import com.medco.HealthConnectProvider.utils.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Schema(description = "Request object for a dependant of an insured person")
@AllArgsConstructor
@NoArgsConstructor
public class DependantRequest {
    @Schema(description = "UUID of the dependant (required for updates, null for new dependants)")
    private String dependantUuid;

    @Schema(description = "First name of the dependant", example = "Filmon")
    private String dependantFirstName;

    @Schema(description = "Father's name of the dependant", example = "Kiros")
    private String dependantFatherName;

    @Schema(description = "Grandfather's name of the dependant", example = "Gher")
    private String dependantGrandFatherName;

    @Schema(description = "Gender of the dependant", example = "Male")
    private String dependantGender;

    @Schema(description = "Birth date of the dependant", example = "2020-01-01")
    private Date dependantBirthDate;

    @Schema(description = "Relationship of the dependant to the insured person", example = "CHILD")
    private Relationship relationship;

    @Schema(description = "Status of the dependant", example = "ACTIVE")
    private Status dependantStatus;

}
