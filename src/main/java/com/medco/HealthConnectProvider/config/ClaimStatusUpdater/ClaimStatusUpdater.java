package com.medco.HealthConnectProvider.config.ClaimStatusUpdater;

import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ClaimStatusUpdater {
private final Map<ClaimStatus,UpdateClaimStatus> statusUpdater;

private ClaimStatusUpdater(SubmittedStatus submitedStatus){
    statusUpdater=new HashMap<>();
    statusUpdater.put(ClaimStatus.SUBMITTED,submitedStatus);

}

    public UpdateClaimStatus getUpdater(ClaimStatus status) {
        UpdateClaimStatus updater = statusUpdater.get(status);
        if (updater == null) {
            throw new IllegalArgumentException("No claim status updater found for status: " + status);
        }
        return updater;
    }


}
