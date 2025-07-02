package com.medco.HealthConnectProvider.entity.claims;

import com.medco.HealthConnectProvider.shared.Audit;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity


@Table(name = "claims", indexes = {
        @Index(name = "idx_claim_payer_status", columnList = "payer_uuid, status"),
        @Index(name = "idx_claim_provider_status", columnList = "provider_uuid, status"),
        @Index(name = "idx_claim_payer_provider_status", columnList = "payer_uuid, provider_uuid, status")
})
public class Claim extends Audit {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String claimUuid = UUID.randomUUID().toString();

    @Column(unique = true)
    private Long claimNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ClaimStatus status;

    @Column(name = "provider_uuid")
    private String providerUuid;

    @Column(name = "payer_uuid")
    private String payerUuid;

    @Column(nullable = false)
    private String mrnNumber;

    @Column(nullable = false)
    private LocalDateTime visitDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

//    @Column(nullable = false)
    private String claimType;

    @Column(nullable = false)
    private LocalDate serviceDate;

    @Column(length = 1000)
    private String providerComment;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime submissionDate;

    @Column(nullable = false)
    private String submittedByUuid;

    @Column(nullable = false)
    private String submittedByName;

    private String preparedByProviderUuid;

    private String preparedByProviderStatus;

    private LocalDateTime preparedByProviderDate;

    private String approvedByProviderUuid;
    private String approvedByProviderStatus;
    private LocalDateTime approvedByProviderDate;

    private String approvedByPayerUuid;
    private String approvedByPayerStatus;
    private LocalDateTime approvedByPayerDate;

    private String paymentRequestedByUuid;
    private LocalDateTime paymentRequestedDate;
    private String paidByPayerUuid;
    private String paidByPayerName;
    private LocalDateTime paidDate;
    private String paidStatus;
    private String paymentCode;
    private String paymentType;
    private String checkNumber;
    private String fromBank;
    private String toBank;
    private String transactionNumber;

    private String cancelledByUuid;
    private LocalDateTime cancelledDate;

    @Column( precision = 10, scale = 2)
    private BigDecimal copayAmount;

    @Column( precision = 10, scale = 2)
    private BigDecimal deductibleAmount;

//    @Column( precision = 10, scale = 2)
//    private BigDecimal coinsuranceAmount;
//
//    @Column(nullable = false, precision = 10, scale = 2)
//    private BigDecimal payerAmount;

//    @Column(nullable = false)
    private String diagnosisCodes;

//    @Column(nullable = false)
    private String procedureCodes;

    @Column(length = 1000)
    private String reviewComment;

    @Column
    private String reviewedByUuid;

    @Column
    private Instant reviewedAt;

//    @OneToOne(mappedBy = "claim")
//    private BatchRecord batchRecord;
    @OneToOne
    @JoinColumn(name = "batch_record_id")  // Add this column to your claims table
    private BatchRecord batchRecord;

//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "contract_id", nullable = false)
//    private ContractHeader contract;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "provider_id", nullable = false)
//    private Provider provider;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "payer_id", nullable = false)
//    private Payer payer;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "insured_id", nullable = false)
//    private Insured insuredPerson;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "dependant_id")
//    private Dependant dependant;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClaimAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClaimComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClaimLogs> logs = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClaimPayment> payments = new ArrayList<>();

//    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<ProvidedService> providedServices = new ArrayList<>();



//    @OneToOne(cascade = CascadeType.ALL)
//    @JoinColumn(name = "provided_service_id")
//    private ProvidedService providedService;


//    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime lastUpdated;


//
//    @PrePersist
//    public void prePersist() {
//        if (status == null) {
//            status = ClaimStatus.SUBMITTED;
//        }
//
//        if (claimUuid == null) {
//            claimUuid = UUID.randomUUID().toString();
//        }
//
//        if (providerUuid == null && provider != null) {
//            providerUuid = provider.getProviderUuid();
//        }
//
//        if (payerUuid == null && payer != null) {
//            payerUuid = payer.getPayerUuid();
//        }
//
//        if (reviewedAt == null) {
//            reviewedAt = Instant.now();
//        }
//    }

//    // Helper methods to get related entity UUIDs
//    public String getContractUuid() {
//        return contract != null ? contract.getContractHeaderUuid() : null;
//    }
//
//    public String getProviderUuid() {
//        return provider != null ? provider.getProviderUuid() : providerUuid;
//    }
//
//    public String getPayerUuid() {
//        return payer != null ? payer.getPayerUuid() : payerUuid;
//    }
//
//    public String getInsuredPersonUuid() {
//        return insuredPerson != null ? insuredPerson.getInsuredUuid() : null;
//    }
//
//    public String getDependantUuid() {
//        return dependant != null ? dependant.getDependantUuid() : null;
//    }
//
//    // Helper methods to get related entity names/details
//    public String getContractName() {
//        return contract != null ? contract.getContractName() : null;
//    }
//
//    public String getContractCode() {
//        return contract != null ? contract.getContractCode() : null;
//    }
//
//    public String getProviderName() {
//        return provider != null ? provider.getProviderName() : null;
//    }
//
//    public String getProviderCode() {
//        return provider != null ? provider.getProviderCode() : null;
//    }
//
//    public String getPayerName() {
//        return payer != null ? payer.getPayerName() : null;
//    }
//
//    public String getPayerCode() {
//        return payer != null ? payer.getPayerCode() : null;
//    }
//
//    public String getInsuredPersonName() {
//        return insuredPerson != null ? insuredPerson.getFirstName() + " " + insuredPerson.getFatherName() : null;
//    }
//
//    public String getInsuredPersonCode() {
//        return insuredPerson != null ? insuredPerson.getInsuranceId() : null;
//    }
//
//    public String getInsuredPersonPhone() {
//        return insuredPerson != null ? insuredPerson.getPhone() : null;
//    }
//
//    public String getInsuredPersonGender() {
//        return insuredPerson != null ? insuredPerson.getGender() : null;
//    }
//
//    public Date getInsuredBirthDate() {
//        return insuredPerson != null ? insuredPerson.getBirthDate() : null;
//    }
//
//    public String getDependantFullName() {
//        return dependant != null ? dependant.getFirstName() + " " + dependant.getFatherName() : null;
//    }
//
//    public String getDependantRelationship() {
//        return dependant != null ? dependant.getRelationship().toString() : null;
//    }
//
//    public String getDependantGender() {
//        return dependant != null ? dependant.getGender() : null;
//    }
//
//    public Date getDependantBirthDate() {
//        return dependant != null ? dependant.getBirthDate() : null;
//    }

    // Helper methods for bidirectional relationship management
    public void addAttachment(ClaimAttachment attachment) {
        attachments.add(attachment);
        attachment.setClaim(this);
    }

    public void removeAttachment(ClaimAttachment attachment) {
        attachments.remove(attachment);
        attachment.setClaim(null);
    }

    public void addComment(ClaimComment comment) {
        comments.add(comment);
        comment.setClaim(this);
    }

    public void removeComment(ClaimComment comment) {
        comments.remove(comment);
        comment.setClaim(null);
    }

    public void addLog(ClaimLogs log) {
        logs.add(log);
        log.setClaim(this);
        log.setClaimUuid(this.claimUuid);
    }

    public void removeLog(ClaimLogs log) {
        logs.remove(log);
        log.setClaim(null);
    }

    public void addPayment(ClaimPayment payment) {
        payments.add(payment);
        payment.setClaim(this);
    }

    public void removePayment(ClaimPayment payment) {
        payments.remove(payment);
        payment.setClaim(null);
    }

//    public void addProvidedService(ProvidedService service) {
//        providedServices.add(service);
//        service.setClaim(this);
//        service.setClaimUuid(this.claimUuid);
//    }
//
//    public void removeProvidedService(ProvidedService service) {
//        providedServices.remove(service);
//        service.setClaim(null);
//        service.setClaimUuid(null);
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Claim)) return false;
        return id != null && id.equals(((Claim) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}