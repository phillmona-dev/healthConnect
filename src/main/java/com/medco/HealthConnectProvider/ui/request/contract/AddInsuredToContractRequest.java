package com.medco.HealthConnectProvider.ui.request.contract;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class AddInsuredToContractRequest {

    @NotNull
    @NotEmpty
    private List<String> insuredUuids;

    private List<DependantRequest> dependants;

    @Data
    public static class DependantRequest {
        @NotNull
        private String insuredUuid;
        @NotNull
        private String dependantUuid;
    }
}
