package com.medco.HealthConnectProvider.ui.response.groups;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GroupSummary {
    private String groupUuid;
    private String groupName;
    private long totalInsured;
}
