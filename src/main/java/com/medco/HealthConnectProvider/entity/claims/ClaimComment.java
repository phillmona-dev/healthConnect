package com.medco.HealthConnectProvider.entity.claims;

import com.medco.HealthConnectProvider.shared.Audit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.util.Date;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "claim_comments")
public class ClaimComment extends Audit {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String commentUuid = UUID.randomUUID().toString();

    @ManyToOne
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Column(nullable = false, length = 500)
    private String comment;

    private String commentType; // PROVIDER, PAYER, PROVIDER_REVIEW, PAYER_REVIEW

    @Column(nullable = false)
    private Date commentDate;

    @Column(nullable = false)
    private String commentByUuid;

    private String commentByName;

    private String commentByRole;
}