package com.medco.HealthConnectProvider.ui.request.search;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.Data;

@Data
public class ProviderSearchRequest {
    private String search;
    private Status status;
    private String category;
    private String providerName;
    private Long tinNumber;
    private String level;
    private String sortBy = "id";
    private String sortDir = "desc";
    private int page = 1;
    private int limit = 25;
}