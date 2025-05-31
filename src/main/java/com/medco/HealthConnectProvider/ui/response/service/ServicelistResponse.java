package com.medco.HealthConnectProvider.ui.response.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ServicelistResponse implements Serializable {
    private String serviceUuid;
    private String itemCode;
    private String item;
    private String subCategory;
    private String category;
    double price;
    private String status;
    private int totalPages;
}
