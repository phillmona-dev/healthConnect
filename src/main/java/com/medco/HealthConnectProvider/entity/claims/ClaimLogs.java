package com.medco.HealthConnectProvider.entity.claims;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
public class ClaimLogs  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Size(min = 36, max = 40)
    private String claimUuid;

    @Size(min = 36, max = 40)
    private String actionByUuid;

    @NotBlank
    @Size(min = 2, max = 500)
    private String comment;

    private Instant actionDate;

    private String actionStatus;

    private String previousStatus;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "claimId",referencedColumnName = "id",updatable = false)
    @JsonBackReference
    private Claim claim;

}
