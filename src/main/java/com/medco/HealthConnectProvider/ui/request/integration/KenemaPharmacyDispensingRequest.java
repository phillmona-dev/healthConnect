package com.medco.HealthConnectProvider.ui.request.integration;


import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KenemaPharmacyDispensingRequest {
    private String cbhid;
    private String identifier;
    private String mrn;
    private String providerBranchName;
    private LocalDate dispensedDate;
    private String physicianFullName;
    private String providerType;
    private String providerName;
    private Double totalPrice;
    private List<PrescriptionDetail> prescriptionDetails;

    @Data
    @Setter
    @Getter
    public static class PrescriptionDetail {

        private String description;
        private String outOfStock;
        private String medicationName;
        private Integer quantity;
        private String unitOfMeasure;
        private Double price;
        private Double dosage;
        private String route;
        private String frequency;
        private String duration;

    }
}
