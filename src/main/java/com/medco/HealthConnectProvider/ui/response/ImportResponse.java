package com.medco.HealthConnectProvider.ui.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class ImportResponse {
    private String message;
    private List<String> errors;

    public ImportResponse(String message, List<String> errors) {
        this.message = message;
        this.errors = errors;
    }

}
