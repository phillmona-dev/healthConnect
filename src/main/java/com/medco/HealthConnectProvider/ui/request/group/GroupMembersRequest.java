package com.medco.HealthConnectProvider.ui.request.group;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class GroupMembersRequest {

    private List<String> dependantUuids;
    private List <String> insuredUuids;
    private boolean isInsured;
}
