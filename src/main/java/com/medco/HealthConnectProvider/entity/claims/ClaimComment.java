package com.medco.HealthConnectProvider.entity.claims;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "claim_comments")
public class ClaimComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 60)
    private String batchCode;
    @Size(max = 500)
    private String processorComment;
    @Size(max = 500)
    private String checkerComment;
    @Size(max = 500)
    private String approverComment;
    @Size(max = 500)
    private String authorizerComment;
}
