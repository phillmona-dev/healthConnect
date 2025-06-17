package com.medco.HealthConnectProvider.ui.response.persons;

import lombok.Data;

import java.util.List;

@Data
public class MultipleInsuredResponse {
    private String message;
    private List<InsuredSearchResponse> insuredPersons;

    public MultipleInsuredResponse(List<InsuredSearchResponse> insuredPersons) {
        this.message = "Multiple insured persons found. Please select one.";
        this.insuredPersons = insuredPersons;
    }
}
