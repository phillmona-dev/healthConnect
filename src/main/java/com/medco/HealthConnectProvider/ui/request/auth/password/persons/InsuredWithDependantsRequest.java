package com.medco.HealthConnectProvider.ui.request.auth.password.persons;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.Valid;
import lombok.Data;

@Data
public class InsuredWithDependantsRequest {
    // Fields from the example JSON
    private String insuredPersonUuid;
    private String insuredTitle;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String gender;
    private String insuranceId;
    private String phone;
    private String email;
    private Date birthDate;
    private Status status;
    private String address1;
    private String address2;
    private String address3;
    private String payerUuid;

    // Dependants list
    @Valid
    private List<DependantRequest> dependants = new ArrayList<>();

    // Pagination metadata (not used for updates but included in the response)
    private long totalPages;
    private long totalElements;
}