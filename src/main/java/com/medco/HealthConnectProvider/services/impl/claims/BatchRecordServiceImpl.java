package com.medco.HealthConnectProvider.services.impl.claims;

import com.medco.HealthConnectProvider.dto.BatchRecordDTO;
import com.medco.HealthConnectProvider.entity.claims.BatchRecord;
import com.medco.HealthConnectProvider.repository.claims.BatchRecordRepository;
import com.medco.HealthConnectProvider.services.claims.BatchRecordService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class BatchRecordServiceImpl implements BatchRecordService {

    private final BatchRecordRepository batchRecordRepository;

    @Autowired
    public BatchRecordServiceImpl(BatchRecordRepository batchRecordRepository) {
        this.batchRecordRepository = batchRecordRepository;
    }

    @Override
    public Page<BatchRecordDTO> searchBatchRecords(
            String search, LocalDateTime requestedOnStart, LocalDateTime requestedOnEnd,
            LocalDate claimDatingFrom, LocalDate claimDatingTo,
            int page, int size, String sortBy, String sortDirection
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<BatchRecord> spec = Specification.where(null);

        if (search != null && !search.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                String likeSearch = "%" + search.toLowerCase() + "%";
                return cb.or(
                        cb.like(cb.lower(root.get("batchCode")), likeSearch),
                        cb.like(cb.lower(root.get("payerName")), likeSearch),
                        cb.like(cb.lower(root.get("status")), likeSearch),
                        cb.like(cb.lower(root.get("claim").get("claimUuid")), likeSearch),
                        cb.like(cb.lower(root.get("totalAmount").as(String.class)), likeSearch)
                );
            });
        }

        if (requestedOnStart != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("requestedOn"), requestedOnStart));
        }
        if (requestedOnEnd != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("requestedOn"), requestedOnEnd));
        }
        if (claimDatingFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("claimDatingFrom"), claimDatingFrom));
        }
        if (claimDatingTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("claimDatingTo"), claimDatingTo));
        }

        Page<BatchRecord> batchRecords = batchRecordRepository.findAll(spec, pageable);
        return batchRecords.map(this::convertToDTO);
    }

    private BatchRecordDTO convertToDTO(BatchRecord batchRecord) {
        BatchRecordDTO dto = new BatchRecordDTO();
        BeanUtils.copyProperties(batchRecord, dto);
        if (batchRecord.getClaim() != null) {
            dto.setClaimUuid(batchRecord.getClaim().getClaimUuid());
        }
        return dto;
    }
}
