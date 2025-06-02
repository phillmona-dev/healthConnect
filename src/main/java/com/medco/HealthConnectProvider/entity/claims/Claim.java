package com.medco.HealthConnectProvider.entity.claims;

import com.medco.HealthConnectProvider.entity.services.ProvidedService;
import com.medco.HealthConnectProvider.shared.Audit;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "claims")
public class Claim extends Audit {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String claimUuid = UUID.randomUUID().toString();

    @Column(nullable = false, unique = true)
    private Long claimNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ClaimStatus status;

    // Add explicit UUID columns for repository queries
    @Column(name = "provider_uuid")
    private String providerUuid;

    @Column(name = "payer_uuid")
    private String payerUuid;

    // Claim details
    @Column(nullable = false)
    private String mrnNumber;

    @Column(nullable = false)
    private Date visitDate;

    @Column(nullable = false)
    private Double totalAmount;

    private String providerComment;

    // Submission information
    private Date submissionDate;
    private String submittedByUuid;
    private String submittedByName;

    // Provider approval information
    private String preparedByProviderUuid;
    private String preparedByProviderStatus;
    private Instant preparedByProviderDate;

    private String approvedByProviderUuid;
    private String approvedByProviderStatus;
    private Instant approvedByProviderDate;

    // Payer approval information
    private String approvedByPayerUuid;
    private String approvedByPayerStatus;
    private Instant approvedByPayerDate;

    // Payment information
    private String paymentRequestedByUuid;
    private Date paymentRequestedDate;
    private String paidByPayerUuid;
    private String paidByPayerName;
    private Date paidDate;
    private String paidStatus;
    private String paymentCode;
    private String paymentType;
    private String checkNumber;
    private String fromBank;
    private String toBank;
    private String transactionNumber;

    // Cancellation information
    private String cancelledByUuid;
    private Date cancelledDate;

    // Relationships with other entities
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private ContractHeader contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id")
    private Payer payer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insured_id")
    private Insured insuredPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependant_id")
    private Dependant dependant;

    // Child entity relationships
    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClaimAttachment> attachments;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClaimComment> comments;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClaimLogs> logs;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClaimPayment> payments;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProvidedService> providedServices;

    @PrePersist
    public void prePersist() {
        if (submissionDate == null) {
            submissionDate = Date.from(Instant.now());
        }

        if (status == null) {
            status = ClaimStatus.SUBMITTED;
        }

        if (claimUuid == null) {
            claimUuid = UUID.randomUUID().toString();
        }

        // Set the UUID fields from the related entities if they're not already set
        if (providerUuid == null && provider != null) {
            providerUuid = provider.getProviderUuid();
        }

        if (payerUuid == null && payer != null) {
            payerUuid = payer.getPayerUuid();
        }
    }

    // Helper methods to get related entity UUIDs
    public String getContractUuid() {
        return contract != null ? contract.getContractHeaderUuid() : null;
    }

    public String getProviderUuid() {
        return provider != null ? provider.getProviderUuid() : providerUuid;
    }

    public String getPayerUuid() {
        return payer != null ? payer.getPayerUuid() : payerUuid;
    }

    public String getInsuredPersonUuid() {
        return insuredPerson != null ? insuredPerson.getInsuredUuid() : null;
    }

    public String getDependantUuid() {
        return dependant != null ? dependant.getDependantUuid() : null;
    }

    // Helper methods to get related entity names/details
    public String getContractName() {
        return contract != null ? contract.getContractName() : null;
    }

    public String getContractCode() {
        return contract != null ? contract.getContractCode() : null;
    }

    public String getProviderName() {
        return provider != null ? provider.getProviderName() : null;
    }

    public String getProviderCode() {
        return provider != null ? provider.getProviderCode() : null;
    }

    public String getPayerName() {
        return payer != null ? payer.getPayerName() : null;
    }

    public String getPayerCode() {
        return payer != null ? payer.getPayerCode() : null;
    }

    public String getInsuredPersonName() {
        return insuredPerson != null ? insuredPerson.getFirstName() + " " + insuredPerson.getFatherName() : null;
    }

    public String getInsuredPersonCode() {
        return insuredPerson != null ? insuredPerson.getInsuranceId() : null;
    }

    public String getInsuredPersonPhone() {
        return insuredPerson != null ? insuredPerson.getPhone() : null;
    }

    public String getInsuredPersonGender() {
        return insuredPerson != null ? insuredPerson.getGender() : null;
    }

    public Date getInsuredBirthDate() {
        return insuredPerson != null ? insuredPerson.getBirthDate() : null;
    }

    public String getDependantFullName() {
        return dependant != null ? dependant.getFirstName() + " " + dependant.getFatherName() : null;
    }

    public String getDependantRelationship() {
        return dependant != null ? dependant.getRelationship().toString() : null;
    }

    public String getDependantGender() {
        return dependant != null ? dependant.getGender() : null;
    }

    public Date getDependantBirthDate() {
        return dependant != null ? dependant.getBirthDate() : null;
    }

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

    public void addProvidedService(ProvidedService service) {
        providedServices.add(service);
        service.setClaim(this);
        service.setClaimUuid(this.claimUuid);
    }

    public void removeProvidedService(ProvidedService service) {
        providedServices.remove(service);
        service.setClaim(null);
        service.setClaimUuid(null);
    }
}