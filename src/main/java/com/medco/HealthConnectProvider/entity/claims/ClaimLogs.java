package com.medco.HealthConnectProvider.entity.claims;

import com.medco.HealthConnectProvider.shared.Audit;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "claim_logs")
public class ClaimLogs extends Audit {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String logUuid = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String claimUuid;

    @ManyToOne
    @JoinColumn(name = "claim_id",referencedColumnName = "id", nullable = false)
    private Claim claim;

    private String actionByUuid;

    private String actionByName;

    private String actionByRole;

    @Column(nullable = false)
    private Instant actionDate;

    @Column(nullable = false)
    private String actionStatus;

    private String previousStatus;

    @Column(length = 500)
    private String comment;


    public ClaimLogs(Claim claim, ClaimStatus newStatus, String comment) {
        this.claim = claim;
        this.claimUuid = claim.getClaimUuid();
        this.actionStatus = newStatus.name();
        this.previousStatus = claim.getStatus().name();
        this.comment = comment;
        this.actionDate = Instant.now();
    }
}