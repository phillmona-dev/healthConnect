package com.medco.HealthConnectProvider.repository.claims;

import com.medco.HealthConnectProvider.entity.claims.ClaimComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimCommentRepository extends JpaRepository<ClaimComment, Long> {
    List<ClaimComment> findByClaimClaimUuidOrderByCommentDateDesc(String claimUuid);
    int countByClaimClaimUuid(String claimUuid);
}