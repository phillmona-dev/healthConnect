package com.medco.HealthConnectProvider.services.impl.dashboard;

import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.repository.claims.ClaimRepository;
import com.medco.HealthConnectProvider.repository.group.EmployeeDependantGroupRepository;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
import com.medco.HealthConnectProvider.services.dashboard.DashboardService;
import com.medco.HealthConnectProvider.ui.response.claims.ClaimStatistics;
import com.medco.HealthConnectProvider.ui.response.dashboard.DashboardResponse;
import com.medco.HealthConnectProvider.ui.response.groups.GroupSummary;
import com.medco.HealthConnectProvider.ui.response.payer.PayerSummary;
import com.medco.HealthConnectProvider.ui.response.provider.ProviderSummary;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final PayerRepository payerRepository;
    private final ProviderRepository providerRepository;
    private final InsuredRepository insuredRepository;
    private final ClaimRepository claimRepository;

    @Autowired
    private EmployeeDependantGroupRepository groupRepository;

    @Autowired
    public DashboardServiceImpl(PayerRepository payerRepository,
                                ProviderRepository providerRepository,
                                InsuredRepository insuredRepository,
                                ClaimRepository claimRepository) {
        this.payerRepository = payerRepository;
        this.providerRepository = providerRepository;
        this.insuredRepository = insuredRepository;
        this.claimRepository = claimRepository;
    }

    @Override
    public DashboardResponse generateDashboardReport() {
        DashboardResponse response = new DashboardResponse();

        response.setTotalPayers(payerRepository.count());
        response.setTotalProviders(providerRepository.count());
        response.setTotalInsured(insuredRepository.count());
        response.setTotalClaims(claimRepository.count());
        response.setTotalGroups(groupRepository.count());

        List<PayerSummary> payerSummaries = payerRepository.findAll().stream()
                .map(this::mapToPayerSummary)
                .collect(Collectors.toList());
        response.setPayerSummaries(payerSummaries);

        long numberOfPayersWithInsured = payerSummaries.stream()
                .filter(summary -> summary.getTotalInsured() > 0)
                .count();
        response.setNumberOfPayersWithInsured(numberOfPayersWithInsured);

        List<ProviderSummary> providerSummaries = providerRepository.findAll().stream()
                .map(this::mapToProviderSummary)
                .collect(Collectors.toList());
        response.setProviderSummaries(providerSummaries);

        response.setClaimStatistics(generateClaimStatistics());
        response.setMonthlyClaimTotals(generateMonthlyClaimTotals());

        return response;
    }

    private PayerSummary mapToPayerSummary(Payer payer) {

        PayerSummary summary = new PayerSummary();
        summary.setPayerUuid(payer.getPayerUuid());
        summary.setPayerName(payer.getPayerName());
        summary.setTotalInsured(insuredRepository.countByPayerUuid(payer.getPayerUuid()));
        summary.setTotalClaims(claimRepository.countByPayerUuid(payer.getPayerUuid()));

        Page<EmployeeDependantGroup> groupPage = groupRepository.findByPayerPayerUuid(payer.getPayerUuid(), Pageable.unpaged());

        summary.setTotalGroups(groupPage.getTotalElements());

        List<GroupSummary> groupSummaries = groupPage.getContent().stream()
                .map(this::mapToGroupSummary)
                .collect(Collectors.toList());
        summary.setGroupSummaries(groupSummaries);

        return summary;
    }

    private GroupSummary mapToGroupSummary(EmployeeDependantGroup group) {
        GroupSummary summary = new GroupSummary();
        summary.setGroupUuid(group.getGroupUuid());
        summary.setGroupName(group.getGroupName());
        summary.setTotalInsured(insuredRepository.countByEmployeeDependantGroupUuid(group.getGroupUuid()));
        return summary;
    }

    private ProviderSummary mapToProviderSummary(Provider provider) {

        ProviderSummary summary = new ProviderSummary();
        summary.setProviderUuid(provider.getProviderUuid());
        summary.setProviderName(provider.getProviderName());
        summary.setTotalClaims(claimRepository.countByProviderUuid(provider.getProviderUuid()));
        return summary;

    }

    private ClaimStatistics generateClaimStatistics() {
        ClaimStatistics statistics = new ClaimStatistics();
        statistics.setPendingClaims(claimRepository.countByStatus(ClaimStatus.DRAFT));
        statistics.setApprovedClaims(claimRepository.countByStatus(ClaimStatus.APPROVED));
        statistics.setRejectedClaims(claimRepository.countByStatus(ClaimStatus.REJECTED));
        statistics.setUnderReviewClaims(claimRepository.countByStatus(ClaimStatus.UNDER_REVIEW));
        return statistics;
    }

    private Map<String, Integer> generateMonthlyClaimTotals() {
        Map<String, Integer> monthlyClaimTotals = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        ZoneId zoneId = ZoneId.systemDefault();

        for (int i = 11; i >= 0; i--) {
            LocalDateTime month = now.minusMonths(i);
            String monthKey = month.format(formatter);
            LocalDateTime startOfMonth = month.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfMonth = month.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).minusSeconds(1);

            Instant startInstant = startOfMonth.atZone(zoneId).toInstant();
            Instant endInstant = endOfMonth.atZone(zoneId).toInstant();

            int claimCount = claimRepository.countByCreatedAtBetween(startInstant, endInstant);
            monthlyClaimTotals.put(monthKey, claimCount);
        }

        return monthlyClaimTotals;
    }
}