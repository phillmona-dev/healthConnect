package com.medco.HealthConnectProvider.services.impl.dashboard;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.repository.claims.ClaimRepository;
import com.medco.HealthConnectProvider.repository.contract.ContractRepository;
import com.medco.HealthConnectProvider.repository.group.EmployeeDependantGroupRepository;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
import com.medco.HealthConnectProvider.repository.service.ServicelistRepository;
import com.medco.HealthConnectProvider.services.dashboard.DashboardService;
import com.medco.HealthConnectProvider.ui.response.claims.ClaimStatistics;
import com.medco.HealthConnectProvider.ui.response.dashboard.DashboardResponse;
import com.medco.HealthConnectProvider.ui.response.groups.GroupSummary;
import com.medco.HealthConnectProvider.ui.response.payer.PayerSummary;
import com.medco.HealthConnectProvider.ui.response.provider.ProviderSummary;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final PayerRepository payerRepository;
    private final ProviderRepository providerRepository;
    private final InsuredRepository insuredRepository;
    private final ClaimRepository claimRepository;
    private final ServicelistRepository servicelistRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private EmployeeDependantGroupRepository groupRepository;

    @Autowired
    public DashboardServiceImpl(PayerRepository payerRepository,
                                ProviderRepository providerRepository,
                                InsuredRepository insuredRepository,
                                ClaimRepository claimRepository, ServicelistRepository servicelistRepository) {
        this.payerRepository = payerRepository;
        this.providerRepository = providerRepository;
        this.insuredRepository = insuredRepository;
        this.claimRepository = claimRepository;
        this.servicelistRepository = servicelistRepository;
    }

    @Override
    public DashboardResponse generateDashboardReport() {

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String providerUuid = userDetails.getProviderUuid();
        String payerUuid = userDetails.getPayerUuid();

        if (providerUuid != null && !providerUuid.isEmpty()) {
            return generateProviderReport(providerUuid);
        } else if (payerUuid != null && !payerUuid.isEmpty()) {
            return generatePayerReport(payerUuid);
        } else {
            return generateFullReport();
        }
    }

    private DashboardResponse generateProviderReport(String providerUuid) {

        DashboardResponse response = new DashboardResponse();
        Provider provider = providerRepository.findByProviderUuid(providerUuid);
        if (provider == null) {
            throw new NoSuchElementException("Provider not found");
        }

        response.setTotalProviders(1);
        response.setTotalClaims(claimRepository.countByProviderUuid(providerUuid));

        long totalServices = servicelistRepository.countByProviderId(provider.getId());

        List<ProviderSummary> providerSummaries = new ArrayList<>();
        ProviderSummary summary = mapToProviderSummary(provider);
        summary.setTotalServices(totalServices);
        providerSummaries.add(summary);
        response.setProviderSummaries(providerSummaries);

        response.setClaimStatistics(generateClaimStatisticsForProvider(providerUuid));
        response.setMonthlyClaimTotals(generateMonthlyClaimTotalsForProvider(providerUuid));
        response.setTotalContracts(contractRepository.countByProviderProviderUuid(providerUuid));
        Map<String, Integer> contractSummary = new HashMap<>();
        contractSummary.put(provider.getProviderName(), response.getTotalContracts());
        response.setContractSummaries(contractSummary);

        return response;

    }

    private DashboardResponse generatePayerReport(String payerUuid) {
        DashboardResponse response = new DashboardResponse();
        Payer payer = payerRepository.findByPayerUuid(payerUuid);
        if (payer == null) {
            throw new NoSuchElementException("Payer not found");
        }

        response.setTotalPayers(1);
        response.setTotalInsured(insuredRepository.countByPayerUuid(payerUuid));
        response.setTotalClaims(claimRepository.countByPayerUuid(payerUuid));
        response.setTotalGroups(groupRepository.countByPayerPayerUuid(payerUuid));

        List<PayerSummary> payerSummaries = new ArrayList<>();
        payerSummaries.add(mapToPayerSummary(payer));
        response.setPayerSummaries(payerSummaries);

        response.setClaimStatistics(generateClaimStatisticsForPayer(payerUuid));
        response.setMonthlyClaimTotals(generateMonthlyClaimTotalsForPayer(payerUuid));
        response.setTotalContracts(contractRepository.countByPayerPayerUuid(payerUuid));
        Map<String, Integer> contractSummary = new HashMap<>();
        contractSummary.put(payer.getPayerName(), response.getTotalContracts());
        response.setContractSummaries(contractSummary);

        return response;

    }

    private DashboardResponse generateFullReport() {
        DashboardResponse response = new DashboardResponse();
        response.setTotalPayers((int) payerRepository.count());
        response.setTotalProviders((int) providerRepository.count());
        response.setTotalInsured((int) insuredRepository.count());
        response.setTotalClaims((int) claimRepository.count());
        response.setTotalGroups((int) groupRepository.count());

        List<PayerSummary> payerSummaries = payerRepository.findAll().stream()
                .map(this::mapToPayerSummary)
                .collect(Collectors.toList());
        response.setPayerSummaries(payerSummaries);

        long numberOfPayersWithInsured = payerSummaries.stream()
                .filter(payer -> payer.getTotalInsured() > 0)
                .count();
        response.setNumberOfPayersWithInsured(numberOfPayersWithInsured);

        List<ProviderSummary> providerSummaries = providerRepository.findAll().stream()
                .map(this::mapToProviderSummary)
                .collect(Collectors.toList());
        response.setProviderSummaries(providerSummaries);

        response.setClaimStatistics(generateClaimStatistics());
        response.setMonthlyClaimTotals(generateMonthlyClaimTotals());

        response.setTotalContracts((int) contractRepository.count());

        Map<String, Integer> contractSummaries = new HashMap<>();
        List<Object[]> payerContractCounts = contractRepository.countContractsByPayer();
        for (Object[] result : payerContractCounts) {
            String payerName = (String) result[0];
            Integer contractCount = ((Number) result[1]).intValue();
            contractSummaries.put(payerName, contractCount);
        }
        response.setContractSummaries(contractSummaries);

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
        summary.setTotalServices(servicelistRepository.countByProviderId(provider.getId()));
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

    private ClaimStatistics generateClaimStatisticsForProvider(String providerUuid) {
        ClaimStatistics statistics = new ClaimStatistics();
        statistics.setPendingClaims(claimRepository.countByProviderUuidAndStatus(providerUuid, ClaimStatus.DRAFT));
        statistics.setApprovedClaims(claimRepository.countByProviderUuidAndStatus(providerUuid, ClaimStatus.APPROVED));
        statistics.setRejectedClaims(claimRepository.countByProviderUuidAndStatus(providerUuid, ClaimStatus.REJECTED));
        statistics.setUnderReviewClaims(claimRepository.countByProviderUuidAndStatus(providerUuid, ClaimStatus.UNDER_REVIEW));
        return statistics;
    }

    private ClaimStatistics generateClaimStatisticsForPayer(String payerUuid) {
        ClaimStatistics statistics = new ClaimStatistics();
        statistics.setPendingClaims(claimRepository.countByPayerUuidAndStatus(payerUuid, ClaimStatus.DRAFT));
        statistics.setApprovedClaims(claimRepository.countByPayerUuidAndStatus(payerUuid, ClaimStatus.APPROVED));
        statistics.setRejectedClaims(claimRepository.countByPayerUuidAndStatus(payerUuid, ClaimStatus.REJECTED));
        statistics.setUnderReviewClaims(claimRepository.countByPayerUuidAndStatus(payerUuid, ClaimStatus.UNDER_REVIEW));
        return statistics;
    }

    private Map<String, Integer> generateMonthlyClaimTotals() {
        return generateMonthlyClaimTotals(null, null);
    }

    private Map<String, Integer> generateMonthlyClaimTotalsForProvider(String providerUuid) {
        return generateMonthlyClaimTotals(providerUuid, null);
    }

    private Map<String, Integer> generateMonthlyClaimTotalsForPayer(String payerUuid) {
        return generateMonthlyClaimTotals(null, payerUuid);
    }

    private Map<String, Integer> generateMonthlyClaimTotals(String providerUuid, String payerUuid) {

        Map<String, Integer> monthlyClaimTotals = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 11; i >= 0; i--) {
            LocalDateTime month = now.minusMonths(i);
            String monthKey = month.format(formatter);
            LocalDateTime startOfMonth = month.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfMonth = month.withDayOfMonth(month.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999999999);

            int claimCount;
            if (providerUuid != null) {
                claimCount = claimRepository.countByProviderUuidAndSubmissionDateBetween(providerUuid, startOfMonth, endOfMonth);
            } else if (payerUuid != null) {
                claimCount = claimRepository.countByPayerUuidAndSubmissionDateBetween(payerUuid, startOfMonth, endOfMonth);
            } else {
                claimCount = claimRepository.countBySubmissionDateBetween(startOfMonth, endOfMonth);
            }

            monthlyClaimTotals.put(monthKey, claimCount);
        }

        return monthlyClaimTotals;
    }

}