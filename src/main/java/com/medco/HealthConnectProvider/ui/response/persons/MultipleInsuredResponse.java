package com.medco.HealthConnectProvider.ui.response.persons;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.List;

@Data
public class MultipleInsuredResponse {
    private String message;
    private List<InsuredSearchResponse> insuredPersons;

    public MultipleInsuredResponse() {
        this(Collections.emptyList());
    }

    public MultipleInsuredResponse(List<InsuredSearchResponse> insuredPersons) {
        this.insuredPersons = (insuredPersons != null) ? insuredPersons : Collections.emptyList();
        int size = this.insuredPersons.size();
        if (size == 0) {
            this.message = "No insured persons found.";
        } else if (size == 1) {
            this.message = "One insured person found.";
        } else {
            this.message = "Multiple insured persons found. Please select one.";
        }
    }

    @JsonIgnore
    public boolean isEmpty() {
        return insuredPersons == null || insuredPersons.isEmpty();
    }

    @JsonIgnore
    public int size() {
        return insuredPersons == null ? 0 : insuredPersons.size();
    }

    @JsonIgnore
    public InsuredSearchResponse getFirst() {
        return (insuredPersons != null && !insuredPersons.isEmpty()) ? insuredPersons.get(0) : null;
    }
}
