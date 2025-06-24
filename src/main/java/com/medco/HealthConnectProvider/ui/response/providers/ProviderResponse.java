package com.medco.HealthConnectProvider.ui.response.providers;


import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ProviderResponse  implements Serializable {

    private String threeDigitAcronym;

    private String providerUuid;

    private String email;
    private String providerName;
    private String description;
    private String tinNumber;
    private String telephone;
    private String category;
    private String level;
    private String address1;
    private String state;
    private String country;
    private double latitude;
    private double longitude;
    private String status;
    private long totalPages;
    private String branch;

    private String roleUuid;

    private String logoPath;

    private String logoBase64;

    private Long totalContracts;

}

