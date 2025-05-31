package com.medco.HealthConnectProvider.ui.response.persons;

import java.util.Date;
import java.util.List;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.Data;

@Data
public class InsuredAndDependantCashServiceResponse {

    private String insuredUuid;
    private String title;
    private String firstName;
    private String grandFatherName;
    private String fatherName;
    private String insuranceId;
    private String phone;
    private String Gender;
    private String state;
    private String address3;
    private String address2;
    private String address1;
    private Date beginDate;
    private Date endDate;
    private Status status;
    private List<DependantCashServiceResponse> dependants;

}
