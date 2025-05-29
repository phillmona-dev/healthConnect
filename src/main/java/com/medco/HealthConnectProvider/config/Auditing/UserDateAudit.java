package com.medco.HealthConnectProvider.config.Auditing;

import jakarta.persistence.Column;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.List;

public abstract class UserDateAudit extends DateAudit{
    @Column(updatable = false)
    @CreatedBy
    private String createdBy;

    @LastModifiedDate
    private List<String> updatedBy;
}
