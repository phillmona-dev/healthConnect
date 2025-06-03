package com.medco.HealthConnectProvider.ui.request.search;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.Data;

@Data
public class PayerSearchRequest {
    private String search;
    private Status status;
    private String category;
    private String payerName;
    private Long tinNumber;
    private String sortBy = "id";
    private String sortDir = "desc";
    private int page = 1;
    private int limit = 25;
}