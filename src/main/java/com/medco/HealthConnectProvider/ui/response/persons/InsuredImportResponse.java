package com.medco.HealthConnectProvider.ui.response.persons;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class InsuredImportResponse {
    private List<InsuredResponse> importedInsured;
    private List<String> skippedInsured;
    private List<String> errors;
    private int successfulImports;
    private int skippedImports;
}
