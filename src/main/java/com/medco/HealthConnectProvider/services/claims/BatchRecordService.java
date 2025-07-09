package com.medco.HealthConnectProvider.services.claims;

import com.medco.HealthConnectProvider.dto.BatchRecordDTO;
import com.medco.HealthConnectProvider.ui.request.claims.BatchRecordSearchCriteria;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface BatchRecordService {

    Page<BatchRecordDTO> searchBatchRecords(String status,String search, LocalDateTime requestedOnStart,
                                            LocalDateTime requestedOnEnd, LocalDate claimDatingFrom,
                                            LocalDate claimDatingTo, int page, int size, String sortBy, String sortDirection);
}
