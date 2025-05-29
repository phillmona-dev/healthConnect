package com.medco.HealthConnectProvider.entity.claims;

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
@Table(name = "claim_payments", uniqueConstraints = { @UniqueConstraint(columnNames = "paymentCode")})
public class ClaimPayment extends Audit {

    @Serial
    private static final long serialVersionUID = 4221081290836121794L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 36, max = 40)
    private String claimPaymentUuid = UUID.randomUUID().toString();

    @Size(max = 60)
    private String paymentCode;

    private double amount;
    @Size(max = 40)
    private String checkNumber;

    @Size(max = 40)
    private String paymentType;

    @Size(max = 150)
    private String file;
    private Long fileSize;

}
