package com.medco.HealthConnectProvider.ui.response.contracts;

import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractResponse implements Serializable {

    private String contractUuid;
    private String payerUuid;
    private String providerUuid;
    private String contractName;
    private String contractCode;
    private String description;
    private Date beginDate;
    private Date endDate;
    private String preparedBy;
    private String approvedBy;
    private String status;
    private boolean isDeleted;
    private long totalPages;
}
