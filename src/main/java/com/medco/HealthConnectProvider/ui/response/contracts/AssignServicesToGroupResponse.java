package com.medco.HealthConnectProvider.ui.response.contracts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignServicesToGroupResponse {
    private String message;
    private int assignedCount;
    private int totalCount;
    private List<String> assignedItems;
    private List<String> skippedItems;
}
