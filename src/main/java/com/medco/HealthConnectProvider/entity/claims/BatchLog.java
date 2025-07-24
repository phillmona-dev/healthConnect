package com.medco.HealthConnectProvider.entity.claims;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "batch_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    private BatchRecord batchRecord;

    private String batchCode;
    private String oldStatus;
    private String newStatus;
    private String changedByUuid;
    private String changedByName;
    private String message;
    private LocalDateTime changedAt;
}
