package com.medco.HealthConnectProvider.ui.response.persons;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.Data;

@Data
public class DependantCashServiceResponse {

    private String dependantUuid;
    private String title;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String Gender;
    private Status status;
    private String relationship;

}
