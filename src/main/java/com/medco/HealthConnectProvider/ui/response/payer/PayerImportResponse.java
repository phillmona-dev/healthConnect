package com.medco.HealthConnectProvider.ui.response.payer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayerImportResponse {

    private List<PayerResponse> importedPayers;
    private List<String> skippedPayers;
    private List<String> errors;
    private int successfulImports;
    private int skippedImports;
}
