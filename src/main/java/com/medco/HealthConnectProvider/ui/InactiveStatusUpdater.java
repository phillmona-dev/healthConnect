package com.medco.HealthConnectProvider.ui;

import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class InactiveStatusUpdater {

    @Autowired
    private InsuredRepository insuredRepository;

    @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
    public void updateInactiveStatuses() {
        List<Insured> insuredList = insuredRepository.findByStatusNotAndInactiveDateBefore(Status.INACTIVE, new Date());
        for (Insured insured : insuredList) {
            insured.setStatus(Status.INACTIVE);
            insuredRepository.save(insured);
            log.info("Insured person {} status changed to INACTIVE", insured.getInsuredUuid());
        }
    }
}
