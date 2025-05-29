package com.medco.HealthConnectProvider.entity.claims;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

import com.medco.HealthConnectProvider.shared.Audit;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "claims",
        indexes = {
                @Index(name = "idx_claim_uud", columnList = "claimUuid"),
                @Index(name = "idx_deleted", columnList = "isDeleted"),
//				@Index(name = "idx_insured_institution", columnList = "institutionUuid")
        }
)
public class Claim  extends Audit implements Serializable {

    private static final long serialVersionUID = 640850128570066909L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 36, max = 40)
    private String claimUuid;

    @Size(min = 36, max = 40)
    private String payerUuid;

    @Size(max = 40)
    private String dependantUuid;

    @Size(max = 40)
    private String insuredPersonUuid;

    @Size(min = 36, max = 40)
    private String providerUuid;

    @Size(min = 36, max = 40)
    private String contractUuid;

    @Size(min = 36, max = 50)
    private String mrnNumber;

    private Long tempClaimNumber;
    private Long claimNumber;
    @Size( max = 40)
    private String batchCode;
    @Size( max = 40)
    private String claimCode;

    private double totalAmount;


    @Size(min = 24, max = 40)
    private String paymentCode;

    private String relationship;

    private Date visitDate;

    @Size( max = 40)
    private String preparedByProviderUuid;
    @Size( max = 40)
    private String approvedByProviderUuid;

    private String approvedByPayerUuid;

    @Size(min = 24, max = 40)
    private String paidByPayerUuid;

    @Column(name = "prepared_by_provider_status")
    @ColumnDefault("'Pending'")
    private String preparedByProviderStatus;

    @Column(name = "approved_by_provider_status")
    @ColumnDefault("'Pending'")
    private String approvedByProviderStatus;


    @Column(name = "approved_by_payer_status", columnDefinition = "VARCHAR(25)")
    @ColumnDefault("'Pending'")
    private String approvedByPayerStatus;

    @Column(name = "paid_status", columnDefinition = "VARCHAR(25)")
    @ColumnDefault("'Pending'")
    private String paidStatus;

    @Column(name = "payer_status", columnDefinition = "VARCHAR(25)")
    @ColumnDefault("'Pending'")
    private String payerStatus;

    @CreatedDate
    private Instant preparedByProviderDate;
    private Date approvedByProviderDate;
    private Date approvedByPayerDate;
    private Date paidDate;

    @Size(max = 25)
    private String checkNumber;

    @Size(max = 100)
    private String fromBank;

    @Size(max = 100)
    private String toBank;

    @Size(max = 25)
    private String transactionNumber;

    @Size(max = 500)
    private String providerComment;

    @Size(max = 100)
    private String insuredPersonName;


    @Size(max = 100)
    private String dependantFullName;

    @Size(max = 100)
    private String insuredPersonPhone;


    @Size(max = 100)
    private String institutionName;

    @Size(max = 40)
    private String institutionUuid;

    @Size(max = 50)
    private String ContractCode;

    @Size(max = 100)
    private String institutionPhone;

    @Size(max = 50)
    private String institutionInsuranceNumber;

    @Size(max = 25)
    private String insuranceId;

    @Size(max = 10)
    private String gender;
    @Size(max = 10)
    private String dependantGender;

    private Date insuredBirthDate;
    private Date dependantBirthDate;

    @Size(max = 60)
    private String approverFullName;


    @Size(max = 15)
    private String requestPaymentStatus;
    @Size(max = 40)
    private String requestPaymentByUuid;
    @Size(max = 75)
    private String requestPaymentByFullName;

    private Date requestPaymentDate;

    @Size(max = 60)
    private String payerApproverFullName;

    @Size(max = 60)
    private String paidByFullName;

    @Column(columnDefinition = "boolean default false")
    private boolean isDeleted;

}
