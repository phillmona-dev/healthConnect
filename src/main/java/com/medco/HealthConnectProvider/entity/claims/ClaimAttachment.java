package com.medco.HealthConnectProvider.entity.claims;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.medco.HealthConnectProvider.shared.Audit;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "claim_attachments")
public class ClaimAttachment extends Audit {

    @Serial
    private static final long serialVersionUID = 4221081290836121794L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 36, max = 40)
    private String claimAttachmentUuid = UUID.randomUUID().toString();

    private String claimUuid;

    private String file;
    private String fileName;
    private Long fileSize;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", referencedColumnName = "id")
    @JsonIgnore
    private Claim claim;

}
