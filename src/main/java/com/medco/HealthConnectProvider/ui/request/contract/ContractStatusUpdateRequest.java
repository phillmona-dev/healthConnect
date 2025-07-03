package com.medco.HealthConnectProvider.ui.request.contract;

import lombok.Data;

@Data
public class ContractStatusUpdateRequest {
    private String action; // "APPROVE", "REJECT", "ACTIVATE", "RESUBMIT"
    private String rejectionReason;
    private String remark;
}
