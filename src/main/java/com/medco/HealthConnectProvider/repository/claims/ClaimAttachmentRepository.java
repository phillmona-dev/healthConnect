package com.medco.HealthConnectProvider.repository.claims;

import com.medco.HealthConnectProvider.entity.claims.ClaimAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimAttachmentRepository extends JpaRepository<ClaimAttachment, Long> {
    Optional<ClaimAttachment> findByAttachmentUuid(String attachmentUuid);
    List<ClaimAttachment> findByClaimClaimUuid(String claimUuid);
    int countByClaimClaimUuid(String claimUuid);
}