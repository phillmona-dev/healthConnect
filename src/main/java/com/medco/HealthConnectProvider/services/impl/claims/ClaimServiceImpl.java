package com.medco.HealthConnectProvider.services.impl.claims;


import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.entity.claims.*;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.integration.MedicationDispensing;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.entity.providers.Provider;


import com.medco.HealthConnectProvider.entity.services.Servicelist;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.claims.*;
import com.medco.HealthConnectProvider.repository.contract.ContractRepository;
import com.medco.HealthConnectProvider.repository.integration.MedicationDispensingRepository;
import com.medco.HealthConnectProvider.repository.persons.DependantRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;

import com.medco.HealthConnectProvider.services.claims.ClaimService;
import com.medco.HealthConnectProvider.services.notification.NotificationService;
import com.medco.HealthConnectProvider.services.payment.PaymentService;
import com.medco.HealthConnectProvider.ui.request.claims.ClaimRequest;
import com.medco.HealthConnectProvider.ui.request.claims.ClaimCommentRequest;
import com.medco.HealthConnectProvider.ui.request.claims.ClaimPaymentRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.claims.*;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClaimServiceImpl implements ClaimService {

    @Value("${file.upload-dir-claims}")
    private String uploadDir;


    private final ClaimRepository claimRepository;
    

    private final ClaimAttachmentRepository claimAttachmentRepository;
    

    private final ClaimCommentRepository claimCommentRepository;
    

    private final ClaimLogsRepository claimLogsRepository;
    

    private final ClaimPaymentRepository claimPaymentRepository;
    

    private final ContractRepository contractRepository;
    

    private final InsuredRepository insuredRepository;
    

    private final DependantRepository dependantRepository;
    

    private final ProviderRepository providerRepository;


    private final PaymentService paymentService;

    private final NotificationService notificationService;
    private final BatchRecordRepository batchRecordRepository;
    private final MedicationDispensingRepository medicationDispensingRepository;

    public ClaimServiceImpl(ClaimRepository claimRepository, ClaimAttachmentRepository claimAttachmentRepository, ClaimCommentRepository claimCommentRepository, ClaimLogsRepository claimLogsRepository, ClaimPaymentRepository claimPaymentRepository, ContractRepository contractRepository, InsuredRepository insuredRepository, DependantRepository dependantRepository, ProviderRepository providerRepository, PaymentService paymentService, NotificationService notificationService, BatchRecordRepository batchRecordRepository, MedicationDispensingRepository medicationDispensingRepository) {
        this.claimRepository = claimRepository;
        this.claimAttachmentRepository = claimAttachmentRepository;
        this.claimCommentRepository = claimCommentRepository;
        this.claimLogsRepository = claimLogsRepository;
        this.claimPaymentRepository = claimPaymentRepository;
        this.contractRepository = contractRepository;
        this.insuredRepository = insuredRepository;
        this.dependantRepository = dependantRepository;
        this.providerRepository = providerRepository;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
        this.batchRecordRepository = batchRecordRepository;
        this.medicationDispensingRepository = medicationDispensingRepository;
    }


    @Override
    @Transactional
    public ResponseEntity<?> submitClaim(ClaimRequest claimRequest) {
        return  null;
//        // Get authenticated user
//        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
//        String providerUuid = userDetails.getPayerUuid();
//
//        // Validate contract exists and is active
//        ContractHeader contract = contractRepository.findByContractHeaderUuid(claimRequest.getContractUuid());
//        if (contract == null) {
//            throw new ResourceNotFoundException("Contract", "contractUuid", claimRequest.getContractUuid());
//        }
//
//        if (contract.getStatus() != Status.ACTIVE) {
//            throw new BadRequestException("Contract is not active. Claims can only be submitted for active contracts.");
//        }
//
//        // Validate provider has access to this contract
//        Provider provider = providerRepository.findByProviderUuid(providerUuid);
//        if (provider == null){
//            throw new ResourceNotFoundException("Provider", "providerUuid", providerUuid);
//        }
//
//        if (!contract.getProvider().getProviderUuid().equals(providerUuid)) {
//            throw new BadRequestException("Contract does not belong to this provider");
//        }
//
//        // Validate insured person exists and belongs to the payer
//        Insured insured = insuredRepository.findByInsuredUuid(claimRequest.getInsuredPersonUuid());
//        if(insured == null){
//            throw new ResourceNotFoundException("Insured Person", "insuredPersonUuid",
//                    claimRequest.getInsuredPersonUuid());
//        }
//
//        if (!insured.getPayer().getPayerUuid().equals(contract.getPayer().getPayerUuid())) {
//            throw new BadRequestException("Insured person does not belong to the payer associated with this contract");
//        }
//
//        // Validate dependant if provided
//        Dependant dependant = null;
//        if (claimRequest.getDependantUuid() != null && !claimRequest.getDependantUuid().isEmpty()) {
//            dependant = dependantRepository.findByDependantUuid(claimRequest.getDependantUuid());
//
//            if(dependant==null){
//                throw new ResourceNotFoundException("Dependant", "dependantUuid",
//                        claimRequest.getDependantUuid());
//            }
//
//            if (!dependant.getInsured().getInsuredUuid().equals(insured.getInsuredUuid())) {
//                throw new BadRequestException("Dependant does not belong to the specified insured person");
//            }
//        }
//
//        // Validate provided services
//        List<ProvidedService> providedServices = new ArrayList<>();
//        if (claimRequest.getProvidedServiceUuids() != null && !claimRequest.getProvidedServiceUuids().isEmpty()) {
//            for (String serviceUuid : claimRequest.getProvidedServiceUuids()) {
//                ProvidedService service = providedServiceRepository.findByProvidedServiceUuid(serviceUuid)
//                        .orElseThrow(() -> new ResourceNotFoundException("Provided Service", "providedServiceUuid", serviceUuid));
//
//                // Validate service belongs to this provider
//                if (!service.getContractDetail().getContractHeader().getProvider().getProviderUuid().equals(providerUuid)) {
//                    throw new BadRequestException("Provided service does not belong to this provider");
//                }
//
//                // Validate service is not already claimed
//                if (service.getClaimUuid() != null && !service.getClaimUuid().isEmpty()) {
//                    throw new BadRequestException("Service with UUID " + serviceUuid + " is already claimed");
//                }
//
//                providedServices.add(service);
//            }
//        }
//
//        Claim claim = new Claim();
//        claim.setClaimUuid(UUID.randomUUID().toString());
//        claim.setMrnNumber(claimRequest.getMrnNumber());
//        claim.setVisitDate(claimRequest.getVisitDate());
//        claim.setTotalAmount(BigDecimal.valueOf(claimRequest.getTotalAmount()));
//        claim.setProviderComment(claimRequest.getProviderComment());
//
////        // Set entity relationships
////        claim.setContract(contract);
////        claim.setProvider(provider);
////        claim.setPayer(contract.getPayer());
////        claim.setInsuredPerson(insured);
////
////        if (dependant != null) {
////            claim.setDependant(dependant);
////        }
//
//        // Set claim status
//        claim.setStatus(ClaimStatus.SUBMITTED);
//        claim.setPreparedByProviderUuid(userDetails.getUserUuid());
//        claim.setPreparedByProviderStatus("Submitted");
//        claim.setPreparedByProviderDate(LocalDateTime.from(Instant.now()));
//
//        // Set default statuses
//        claim.setApprovedByProviderStatus("Pending");
//        claim.setApprovedByPayerStatus("Pending");
//        claim.setPaidStatus("Pending");
//
//        // Save claim
//        Claim savedClaim = claimRepository.save(claim);
//
//        // Update provided services with claim UUID
//        for (ProvidedService service : providedServices) {
//            service.setClaimUuid(savedClaim.getClaimUuid());
//            providedServiceRepository.save(service);
//        }
//
//        // Create claim log
//        ClaimLogs log = new ClaimLogs();
//        log.setLogUuid(UUID.randomUUID().toString());
//        log.setClaim(savedClaim);
//        log.setActionByUuid(userDetails.getUserUuid());
//        log.setActionByName(userDetails.getFirstName() + " " + userDetails.getFatherName());
//        log.setActionByRole(userDetails.getAuthorities().iterator().next().getAuthority());
//        log.setComment("Claim submitted by provider");
//        log.setActionDate(Instant.now());
//        log.setActionStatus(ClaimStatus.SUBMITTED.toString());
//        log.setPreviousStatus("New");
//        claimLogsRepository.save(log);
//
//        // Notify claim submission
//        notificationService.notifyClaimSubmitted(savedClaim);
//
//        return ResponseEntity.ok(new MessageResponse("Claim submitted successfully with UUID: " + savedClaim.getClaimUuid()));
    }


    @Override
    public ClaimDetailResponse getClaimByUuid(String claimUuid) {
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));

        // Check if user has access to this claim
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String institutionUuid = userDetails.getPayerUuid();

        // If user is from provider, check if claim belongs to this provider
        if (userDetails.getProviderUuid() == null || !claim.getProviderUuid().equals(userDetails.getProviderUuid())) {
            throw new BadRequestException("You don't have access to this claim");
        }

        // If user is from payer, check if claim belongs to this payer
        if (userDetails.getPayerUuid() == null || !claim.getPayerUuid().equals(institutionUuid)) {
            throw new BadRequestException("You don't have access to this claim");
        }

        // Map claim to response
        ClaimDetailResponse response = new ClaimDetailResponse();
        BeanUtils.copyProperties(claim, response);

        // Set related entity data from relationships
        if (claim.getBatchRecord().getMedicationDispensing()!=null) {
            response.setContractUuid(claim.getBatchRecord().getMedicationDispensing().get(0).getItems().get(0).getContractDetail().getContractHeader().getContractHeaderUuid());
            response.setContractName(claim.getBatchRecord().getMedicationDispensing().get(0).getItems().get(0).getContractDetail().getContractHeader().getContractName());
            response.setContractCode(claim.getBatchRecord().getMedicationDispensing().get(0).getItems().get(0).getContractDetail().getContractHeader().getContractCode());

            Payer payer = claim.getBatchRecord().getMedicationDispensing().get(0).getItems().get(0).getContractDetail().getContractHeader().getPayer();
            Provider provider = claim.getBatchRecord().getMedicationDispensing().get(0).getItems().get(0).getContractDetail().getContractHeader().getProvider();
            if (claim.getBatchRecord().getMedicationDispensing().get(0).getItems().get(0).getContractDetail().getServicelist().getProvider() != null) {
                response.setProviderUuid(claim.getProviderUuid());
                response.setProviderName(provider.getProviderName());
                response.setProviderCode(provider.getProviderCode());
            }

            if (claim.getBatchRecord().getMedicationDispensing().get(0).getItems().get(0).getContractDetail().getContractHeader().getPayer() != null) {
                response.setPayerUuid(claim.getPayerUuid());
                response.setPayerName(payer.getPayerName());
                response.setPayerCode(payer.getPayerCode());
            }
            if (claim.getBatchRecord().getMedicationDispensing().get(0).getInsured() != null) {
                Insured insured = claim.getBatchRecord().getMedicationDispensing().get(0).getInsured();
                response.setInsuredPersonUuid(insured.getInsuredUuid());
                response.setInsuredPersonName(insured.getFirstName() + " " + insured.getFatherName());
//            response.setInsuredPersonCode(insured.get());
                response.setInsuredPersonPhone(insured.getPhone());
                response.setInsuredPersonGender(insured.getGender());
                response.setInsuredPersonUuid(insured.getInsuredUuid());

            }
            if (claim.getBatchRecord().getMedicationDispensing().get(0).getDependant() != null) {
                Dependant dependant = claim.getBatchRecord().getMedicationDispensing().get(0).getDependant();
                response.setDependantUuid(dependant.getDependantUuid());
                response.setDependantFullName(dependant.getFirstName());
                response.setDependantRelationship(dependant.getRelationship().toString());
            }
        }
        // Use the JPA relationships to get related collections
        // Get attachments - can use the relationship directly
        response.setAttachments(claim.getAttachments().stream()
                .map(this::mapToAttachmentResponse)
                .collect(Collectors.toList()));

        // Get comments - can use the relationship directly
        response.setComments(claim.getComments().stream()
                .sorted(Comparator.comparing(ClaimComment::getCommentDate).reversed())
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList()));

        // Get logs - can use the relationship directly
        response.setLogs(claim.getLogs().stream()
                .sorted(Comparator.comparing(ClaimLogs::getActionDate).reversed())
                .map(this::mapToLogResponse)
                .collect(Collectors.toList()));

        // Get provided services - can use the relationship directly
//        response.setService(mapToServiceResponse(claim.getProvidedService()));

        return response;
    }

    @Override
    public List<ClaimResponse> getClaimsByProvider(String providerUuid, Pageable pageable) {

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        if (userDetails.getProviderUuid() != null && !userDetails.getPayerUuid().equals(providerUuid)) {
            throw new BadRequestException("You don't have access to claims from this provider");
        }

        var claimsPage = claimRepository.findByProviderUuid(providerUuid, pageable);
        int totalPages = claimsPage.getTotalPages();

        return claimsPage.getContent().stream()
                .map(claim -> {
                    ClaimResponse response = mapToClaimResponse(claim);
                    response.setTotalPages(totalPages);
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ClaimResponse> getClaimsByPayer(String payerUuid, Pageable pageable) {

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        if (userDetails.getPayerUuid() != null && !userDetails.getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("You don't have access to claims from this payer");
        }

        var claimsPage = claimRepository.findByPayerUuid(payerUuid, pageable);
        int totalPages = claimsPage.getTotalPages();

        return claimsPage.getContent().stream()
                .map(claim -> {
                    ClaimResponse response = mapToClaimResponse(claim);
                    response.setTotalPages(totalPages);
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ClaimResponse> getClaimsByStatus(ClaimStatus status, Pageable pageable) {
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String institutionUuid = userDetails.getPayerUuid();

        var claimsPage = userDetails.getProviderUuid() != null ?
                claimRepository.findByProviderUuidAndStatus(institutionUuid, status, pageable) :
                claimRepository.findByPayerUuidAndStatus(institutionUuid, status, pageable);

        int totalPages = claimsPage.getTotalPages();

        return claimsPage.getContent().stream()
                .map(claim -> {
                    ClaimResponse response = mapToClaimResponse(claim);
                    response.setTotalPages(totalPages);
                    return response;
                })
                .collect(Collectors.toList());
    }

    private ClaimResponse mapToClaimResponse(Claim claim) {
        ClaimResponse response = new ClaimResponse();
        BeanUtils.copyProperties(claim, response);

        int attachmentsCount = claimAttachmentRepository.countByClaimClaimUuid(claim.getClaimUuid());
        int commentsCount = claimCommentRepository.countByClaimClaimUuid(claim.getClaimUuid());

        response.setTotalAttachments(attachmentsCount);
        response.setTotalComments(commentsCount);

        return response;
    }

    private ClaimAttachmentResponse mapToAttachmentResponse(ClaimAttachment attachment) {
        ClaimAttachmentResponse response = new ClaimAttachmentResponse();
        BeanUtils.copyProperties(attachment, response);
        return response;
    }

    private ClaimCommentResponse mapToCommentResponse(ClaimComment comment) {
        ClaimCommentResponse response = new ClaimCommentResponse();
        BeanUtils.copyProperties(comment, response);
        return response;
    }

    private ClaimLogResponse mapToLogResponse(ClaimLogs log) {
        ClaimLogResponse response = new ClaimLogResponse();
        BeanUtils.copyProperties(log, response);
        return response;
    }

//    private ProvidedServiceResponse mapToServiceResponse(ProvidedService service) {
//        ProvidedServiceResponse response = new ProvidedServiceResponse();
//        BeanUtils.copyProperties(service, response);
//
//        if (service.getContractDetail() != null && service.getContractDetail().getServicelist() != null) {
//            Servicelist servicelist = service.getContractDetail().getServicelist();
//            response.setServiceUuid(servicelist.getServiceUuid());
//            response.setServiceName(servicelist.getServiceName());
//            response.setServiceCode(servicelist.getServiceCode());
//            response.setServiceCategory(servicelist.getServiceCategory());
//            response.setServiceSubCategory(servicelist.getServiceSubCategory());
//
//            response.setNegotiatedPrice(service.getContractDetail().getNegotiatedPrice());
//        }
//
//        return response;
//    }

    @Override
    @Transactional
    public ResponseEntity<?> updateClaimStatus(String claimUuid, ClaimStatus newStatus, String comment) {
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        ClaimStatus previousStatus = claim.getStatus();

        // Validate status transition
        validateStatusTransition(claim, newStatus);

        // Update claim status
        claim.setStatus(ClaimStatus.valueOf(newStatus.toString()));

        // Update specific status fields based on the new status
        updateStatusSpecificFields(claim, newStatus, userDetails);

        // Save updated claim
        claimRepository.save(claim);

        // Create claim log
        createClaimLog(claim, userDetails, previousStatus, newStatus, comment);

        return ResponseEntity.ok(new MessageResponse("Claim status updated successfully to " + newStatus));
    }

    @Override
    @Transactional
    public ResponseEntity<?> reviewClaim(String claimUuid, boolean approved, String reviewComment) {
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        ClaimStatus previousStatus = claim.getStatus();
        ClaimStatus newStatus;

        // Determine if this is a provider review or payer review
        boolean isProviderReview = userDetails.getProviderUuid() != null;
        boolean isPayerReview = userDetails.getPayerUuid() != null;

        if (isProviderReview) {
            // Provider review
            if (!claim.getProviderUuid().equals(userDetails.getPayerUuid())) {
                throw new BadRequestException("You don't have access to review this claim");
            }

            if (!previousStatus.equals(ClaimStatus.SUBMITTED.toString())) {
                throw new BadRequestException("Only submitted claims can be reviewed by provider");
            }

            if (approved) {
                claim.setApprovedByProviderUuid(userDetails.getUserUuid());
                claim.setApprovedByProviderStatus("Approved");
                claim.setApprovedByProviderDate(LocalDateTime.from(Instant.now()));
                newStatus = ClaimStatus.UNDER_REVIEW; // Move to payer review
            } else {
                claim.setApprovedByProviderStatus("Rejected");
                newStatus = ClaimStatus.REJECTED;
            }
        } else if (isPayerReview) {
            // Payer review
            if (!claim.getPayerUuid().equals(userDetails.getPayerUuid())) {
                throw new BadRequestException("You don't have access to review this claim");
            }

            if (!previousStatus.equals(ClaimStatus.UNDER_REVIEW.toString())) {
                throw new BadRequestException("Only claims under review can be reviewed by payer");
            }

            if (approved) {
                claim.setApprovedByPayerUuid(userDetails.getUserUuid());
                claim.setApprovedByPayerStatus("Approved");
                claim.setApprovedByPayerDate(LocalDateTime.from(Instant.now()));
                newStatus = ClaimStatus.APPROVED;
            } else {
                claim.setApprovedByPayerStatus("Rejected");
                newStatus = ClaimStatus.REJECTED;
            }
        } else {
            throw new BadRequestException("Only provider or payer users can review claims");
        }

        // Update claim status
        claim.setStatus(ClaimStatus.valueOf(newStatus.toString()));

        // Save updated claim
        claimRepository.save(claim);

        // Create claim log
        String logComment = approved ? "Claim approved" : "Claim rejected";
        if (reviewComment != null && !reviewComment.isEmpty()) {
            logComment += ": " + reviewComment;
        }

        createClaimLog(claim, userDetails, previousStatus, newStatus, logComment);

        // Add comment if provided
        if (reviewComment != null && !reviewComment.isEmpty()) {
            ClaimComment comment = new ClaimComment();
            comment.setCommentUuid(UUID.randomUUID().toString());
            comment.setClaim(claim);
            comment.setComment(reviewComment);
            comment.setCommentType(isProviderReview ? "PROVIDER_REVIEW" : "PAYER_REVIEW");
            comment.setCommentDate(Date.from(Instant.now()));
            comment.setCommentByUuid(userDetails.getUserUuid());
            comment.setCommentByName(userDetails.getFirstName() + " " + userDetails.getFatherName());
            comment.setCommentByRole(userDetails.getAuthorities().iterator().next().getAuthority());

            claimCommentRepository.save(comment);
        }

        return ResponseEntity.ok(new MessageResponse("Claim " + (approved ? "approved" : "rejected") + " successfully"));
    }

    // Helper methods for claim processing
    private void validateStatusTransition(Claim claim, ClaimStatus newStatus) {
        String currentStatus = String.valueOf(claim.getStatus());

        // Define valid transitions
        if (currentStatus.equals(ClaimStatus.SUBMITTED.toString())) {
            if (newStatus != ClaimStatus.UNDER_REVIEW && newStatus != ClaimStatus.REJECTED && newStatus != ClaimStatus.CANCELLED) {
                throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
            }
        } else if (currentStatus.equals(ClaimStatus.UNDER_REVIEW.toString())) {
            if (newStatus != ClaimStatus.APPROVED && newStatus != ClaimStatus.REJECTED && newStatus != ClaimStatus.CANCELLED) {
                throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
            }
        } else if (currentStatus.equals(ClaimStatus.APPROVED.toString())) {
            if (newStatus != ClaimStatus.PAYMENT_REQUESTED && newStatus != ClaimStatus.CANCELLED) {
                throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
            }
        } else if (currentStatus.equals(ClaimStatus.PAYMENT_REQUESTED.toString())) {
            if (newStatus != ClaimStatus.PAID && newStatus != ClaimStatus.CANCELLED) {
                throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
            }
        } else if (currentStatus.equals(ClaimStatus.PAID.toString()) ||
                currentStatus.equals(ClaimStatus.REJECTED.toString()) ||
                currentStatus.equals(ClaimStatus.CANCELLED.toString())) {
            throw new BadRequestException("Cannot change status of a claim that is " + currentStatus);
        }
    }

    private void updateStatusSpecificFields(Claim claim, ClaimStatus newStatus, UserPrincipal userDetails) {
        switch (newStatus) {
            case SUBMITTED:
                claim.setPreparedByProviderUuid(userDetails.getUserUuid());
                claim.setPreparedByProviderStatus("Submitted");
                claim.setPreparedByProviderDate(LocalDateTime.from(Instant.now()));
                break;
            case UNDER_REVIEW:
                // Already handled in reviewClaim method
                break;
            case APPROVED:
                // Already handled in reviewClaim method
                break;
            case PAYMENT_REQUESTED:
                claim.setPaymentRequestedDate(LocalDateTime.from(Instant.now()));
                claim.setPaymentRequestedByUuid(userDetails.getUserUuid());
                break;
            case PAID:
                // Handled in processPayment method
                break;
            case REJECTED:
                // Already handled in reviewClaim method
                break;
            case CANCELLED:
                claim.setCancelledDate(LocalDateTime.from(Instant.now()));
                claim.setCancelledByUuid(userDetails.getUserUuid());
                break;
            default:
                break;
        }
    }

    private void createClaimLog(Claim claim, UserPrincipal userDetails, ClaimStatus previousStatus, ClaimStatus newStatus, String comment) {
        ClaimLogs log = new ClaimLogs();
        log.setLogUuid(UUID.randomUUID().toString());
        log.setClaimUuid(claim.getClaimUuid());
        log.setClaim(claim);
        log.setActionByUuid(userDetails.getUserUuid());
        log.setActionByName(userDetails.getFirstName() + " " + userDetails.getFatherName());
        log.setActionByRole(userDetails.getAuthorities().iterator().next().getAuthority());
        log.setActionDate(Instant.now());
        log.setPreviousStatus(String.valueOf(previousStatus));
        log.setActionStatus(String.valueOf(newStatus));
        log.setComment(comment);

        claimLogsRepository.save(log);
    }


    @Override
    @Transactional
    public ResponseEntity<?> addClaimAttachment(String claimUuid, MultipartFile file) {
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();

        // Check if user has access to this claim
        if (userDetails.getProviderUuid() != null && !claim.getProviderUuid().equals(userDetails.getPayerUuid())) {
            throw new BadRequestException("You don't have access to add attachments to this claim");
        }

        if (userDetails.getPayerUuid() != null && !claim.getPayerUuid().equals(userDetails.getPayerUuid())) {
            throw new BadRequestException("You don't have access to add attachments to this claim");
        }

        // Validate file
        if (file.isEmpty()) {
            throw new BadRequestException("File cannot be empty");
        }

        // Validate file type
        String fileExtension = getFileExtension(file.getOriginalFilename());
        if (!isValidFileExtension(fileExtension)) {
            throw new BadRequestException("Invalid file type. Allowed types: pdf, jpg, jpeg, png, doc, docx, xls, xlsx");
        }

        try {
            // Create directory if it doesn't exist
            File directory = new File(uploadDir + "/" + claimUuid);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Generate unique filename
            String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;
            String filePath = uploadDir + "/" + claimUuid + "/" + uniqueFilename;

            // Save file to disk
            File dest = new File(filePath);
            file.transferTo(dest);

            // Create attachment record
            ClaimAttachment attachment = new ClaimAttachment();
            attachment.setAttachmentUuid(UUID.randomUUID().toString());
            attachment.setClaim(claim);
            attachment.setFileName(file.getOriginalFilename());
            attachment.setFileType(file.getContentType());
            attachment.setFileUrl("/claims/attachments/" + claimUuid + "/" + uniqueFilename);
            attachment.setFileSize(file.getSize());
            attachment.setUploadDate(Date.from(Instant.now()));
            attachment.setUploadedByUuid(userDetails.getUserUuid());
            attachment.setUploadedByName(userDetails.getFirstName() + " " + userDetails.getFatherName());

            claimAttachmentRepository.save(attachment);

            // Create log entry
            createClaimLog(claim, userDetails, claim.getStatus(), claim.getStatus(),
                    "Attachment added: " + file.getOriginalFilename());

            return ResponseEntity.ok(new MessageResponse("Attachment added successfully"));

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteClaimAttachment(String attachmentUuid) {
        ClaimAttachment attachment = claimAttachmentRepository.findByAttachmentUuid(attachmentUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "attachmentUuid", attachmentUuid));

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();

        // Check if user has access to this attachment
        Claim claim = attachment.getClaim();
        if (userDetails.getProviderUuid() != null && !claim.getProviderUuid().equals(userDetails.getPayerUuid())) {
            throw new BadRequestException("You don't have access to delete this attachment");
        }

        if (userDetails.getPayerUuid() != null && !claim.getPayerUuid().equals(userDetails.getPayerUuid())) {
            throw new BadRequestException("You don't have access to delete this attachment");
        }

        // Check if attachment can be deleted (only if claim is not in final state)
        if (claim.getStatus().equals(ClaimStatus.PAID.toString()) ||
                claim.getStatus().equals(ClaimStatus.REJECTED.toString())) {
            throw new BadRequestException("Cannot delete attachments for claims that are paid or rejected");
        }

        try {
            // Delete file from disk
            String filePath = uploadDir + attachment.getFileUrl().replace("/claims/attachments", "");
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }

            // Delete attachment record
            claimAttachmentRepository.delete(attachment);

            // Create log entry
            createClaimLog(claim, userDetails, claim.getStatus(), claim.getStatus(),
                    "Attachment deleted: " + attachment.getFileName());

            return ResponseEntity.ok(new MessageResponse("Attachment deleted successfully"));

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    // Helper methods for file handling
    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }

    private boolean isValidFileExtension(String extension) {
        List<String> allowedExtensions = Arrays.asList("pdf", "jpg", "jpeg", "png", "doc", "docx", "xls", "xlsx");
        return allowedExtensions.contains(extension.toLowerCase());
    }

    @Override
    @Transactional
    public ResponseEntity<?> addClaimComment(String claimUuid, ClaimCommentRequest commentRequest) {
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();

        // Check if user has access to this claim
        if (userDetails.getProviderUuid() != null && !claim.getProviderUuid().equals(userDetails.getPayerUuid())) {
            throw new BadRequestException("You don't have access to add comments to this claim");
        }

        if (userDetails.getPayerUuid() != null && !claim.getPayerUuid().equals(userDetails.getPayerUuid())) {
            throw new BadRequestException("You don't have access to add comments to this claim");
        }

        // Create comment
        ClaimComment comment = new ClaimComment();
        comment.setCommentUuid(UUID.randomUUID().toString());
        comment.setClaim(claim);
        comment.setComment(commentRequest.getComment());
        comment.setCommentType(commentRequest.getCommentType() != null ?
                commentRequest.getCommentType() :
                (userDetails.getProviderUuid() != null ? "PROVIDER" : "PAYER"));
        comment.setCommentDate(Date.from(Instant.now()));
        comment.setCommentByUuid(userDetails.getUserUuid());
        comment.setCommentByName(userDetails.getFirstName() + " " + userDetails.getFatherName());
        comment.setCommentByRole(userDetails.getAuthorities().iterator().next().getAuthority());

        claimCommentRepository.save(comment);

        // Create log entry
        createClaimLog(claim, userDetails, claim.getStatus(), claim.getStatus(),
                "Comment added: " + commentRequest.getComment().substring(0, Math.min(50, commentRequest.getComment().length())) +
                        (commentRequest.getComment().length() > 50 ? "..." : ""));

        return ResponseEntity.ok(new MessageResponse("Comment added successfully"));
    }


    @Override
    @Transactional
    public ResponseEntity<?> requestPayment(String claimUuid) {
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();

        // Check if user has access to this claim
        if (userDetails.getProviderUuid() == null || !claim.getProviderUuid().equals(userDetails.getPayerUuid())) {
            throw new BadRequestException("Only provider users can request payment for claims");
        }

        // Check if claim is in approved status
        if (!claim.getStatus().equals(ClaimStatus.APPROVED.toString())) {
            throw new BadRequestException("Only approved claims can be submitted for payment");
        }

        // Update claim status
        ClaimStatus previousStatus = claim.getStatus();
        claim.setStatus(ClaimStatus.PAYMENT_REQUESTED);
        claim.setPaymentRequestedDate(LocalDateTime.from(Instant.now()));
        claim.setPaymentRequestedByUuid(userDetails.getUserUuid());

        claimRepository.save(claim);

        // Create log entry
        createClaimLog(claim, userDetails, previousStatus, ClaimStatus.PAYMENT_REQUESTED,
                "Payment requested by provider");

        return ResponseEntity.ok(new MessageResponse("Payment request submitted successfully"));
    }

//    @Override
//    @Transactional
//    public ResponseEntity<?> processPayment(String claimUuid, ClaimPaymentRequest paymentRequest) {
//        Claim claim = claimRepository.findByClaimUuid(claimUuid)
//                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));
//
//        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
//
//        // Check if user has access to this claim
//        if (userDetails.getPayerUuid() == null || !claim.getPayerUuid().equals(userDetails.getPayerUuid())) {
//            throw new BadRequestException("Only payer users can process payments for claims");
//        }
//
//        // Check if claim is in payment requested status
//        if (!claim.getStatus().equals(ClaimStatus.PAYMENT_REQUESTED.toString())) {
//            throw new BadRequestException("Only claims with payment requested can be processed for payment");
//        }
//
//        // Validate payment type
//        if (paymentRequest.getPaymentType().equals("CHECK") &&
//                (paymentRequest.getCheckNumber() == null || paymentRequest.getCheckNumber().isEmpty())) {
//            throw new BadRequestException("Check number is required for check payments");
//        }
//
//        // Create payment record
//        ClaimPayment payment = new ClaimPayment();
//        payment.setPaymentUuid(UUID.randomUUID().toString());
//        payment.setClaim(claim);
//        payment.setAmount(paymentRequest.getAmount());
//        payment.setPaymentType(paymentRequest.getPaymentType());
//        payment.setCheckNumber(paymentRequest.getCheckNumber());
//        payment.setFromBank(paymentRequest.getFromBank());
//        payment.setToBank(paymentRequest.getToBank());
//        payment.setTransactionNumber(paymentRequest.getTransactionNumber());
//        payment.setPaymentDate(Date.from(Instant.now()));
//        payment.setPaidByUuid(userDetails.getUserUuid());
//        payment.setPaidByName(userDetails.getFirstName() + " " + userDetails.getFatherName());
//
//        claimPaymentRepository.save(payment);
//
//        // Update claim status
//        ClaimStatus previousStatus = claim.getStatus();
//        claim.setStatus(ClaimStatus.PAID);
//        claim.setPaidStatus("Paid");
//        claim.setPaidDate(LocalDateTime.from(Instant.now()));
//        claim.setPaidByPayerUuid(userDetails.getUserUuid());
//        claim.setPaidByPayerName(userDetails.getFirstName() + " " + userDetails.getFatherName());
//        claim.setPaymentCode(payment.getPaymentUuid());
//        claim.setCheckNumber(paymentRequest.getCheckNumber());
//        claim.setFromBank(paymentRequest.getFromBank());
//        claim.setToBank(paymentRequest.getToBank());
//        claim.setTransactionNumber(paymentRequest.getTransactionNumber());
//
//        claimRepository.save(claim);
//
//        // Create log entry
//        createClaimLog(claim, userDetails, previousStatus, ClaimStatus.PAID,
//                "Payment processed by payer. Amount: " + paymentRequest.getAmount() +
//                        ", Type: " + paymentRequest.getPaymentType() +
//                        (paymentRequest.getCheckNumber() != null ? ", Check #: " + paymentRequest.getCheckNumber() : "") +
//                        (paymentRequest.getTransactionNumber() != null ? ", Transaction #: " + paymentRequest.getTransactionNumber() : ""));
//
//        return ResponseEntity.ok(new MessageResponse("Payment processed successfully"));
//    }

    @Override
    public List<?> getClaimLogs(String claimUuid, Pageable pageable) {
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();

        // Check if user has access to this claim
        if (userDetails.getProviderUuid() != null && !claim.getProviderUuid().equals(userDetails.getPayerUuid())) {
            throw new BadRequestException("You don't have access to view logs for this claim");
        }

        if (userDetails.getPayerUuid() != null && !claim.getPayerUuid().equals(userDetails.getPayerUuid())) {
            throw new BadRequestException("You don't have access to view logs for this claim");
        }

        var logsPage = claimLogsRepository.findByClaimClaimUuidOrderByActionDateDesc(claimUuid, pageable);

        return logsPage.getContent().stream()
                .map(this::mapToLogResponse)
                .collect(Collectors.toList());
    }

    //payment

    @Override
    @Transactional
    public ResponseEntity<?> processPayment(String claimUuid, ClaimPaymentRequest paymentRequest) {
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();

        // Check if user has access to this claim
        if (userDetails.getPayerUuid() == null || !claim.getPayerUuid().equals(userDetails.getPayerUuid())) {
            throw new BadRequestException("Only payer users can process payments for claims");
        }

        // Check if claim is in approved status
        if (!claim.getStatus().equals(ClaimStatus.APPROVED.toString())) {
            throw new BadRequestException("Only approved claims can be processed for payment");
        }

        // Initiate payment through Chapa
        String paymentResponse = String.valueOf(paymentService.initiatePayment(claimUuid, BigDecimal.valueOf(paymentRequest.getAmount()), "ETB"));

        // Create payment record
        ClaimPayment payment = new ClaimPayment();
        payment.setPaymentUuid(UUID.randomUUID().toString());
        payment.setClaim(claim);
        payment.setAmount(paymentRequest.getAmount());
        payment.setPaymentType("CHAPA");
        payment.setTransactionNumber(paymentResponse); // Assuming the response contains the transaction ID
        payment.setPaymentDate(Date.from(Instant.now()));
        payment.setPaidByUuid(userDetails.getUserUuid());
        payment.setPaidByName(userDetails.getFirstName() + " " + userDetails.getFatherName());

        claimPaymentRepository.save(payment);

        // Update claim status
        ClaimStatus previousStatus = claim.getStatus();
        claim.setStatus(ClaimStatus.PAYMENT_INITIATED);
        claim.setPaidStatus("Payment Initiated");
        claim.setPaidDate(LocalDateTime.from(Instant.now()));
        claim.setPaidByPayerUuid(userDetails.getUserUuid());
        claim.setPaidByPayerName(userDetails.getFirstName() + " " + userDetails.getFatherName());
        claim.setPaymentCode(payment.getPaymentUuid());
        claim.setTransactionNumber(paymentResponse);

        claimRepository.save(claim);

        // Create log entry
        createClaimLog(claim, userDetails, previousStatus, ClaimStatus.PAYMENT_INITIATED,
                "Payment initiated through Chapa. Amount: " + paymentRequest.getAmount());

        // Notify the pharmacy
//        notificationService.notifyPharmacy(claim.getProvidedService().getContractDetail().getServicelist().getProvider(), "Payment initiated for claim " + claimUuid);

        return ResponseEntity.ok(new MessageResponse("Payment initiated successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> verifyPayment(String claimUuid) {
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));

        if (!claim.getStatus().equals(ClaimStatus.PAYMENT_INITIATED.toString())) {
            throw new BadRequestException("Payment verification can only be done for claims with initiated payments");
        }

        boolean paymentVerified = paymentService.verifyPayment(claim.getTransactionNumber());

        if (paymentVerified) {
            ClaimStatus previousStatus = claim.getStatus();
            claim.setStatus(ClaimStatus.PAID);
            claim.setPaidStatus("Paid");
            claimRepository.save(claim);

            createClaimLog(claim, SecurityUtils.getAuthenticatedUser(), previousStatus, ClaimStatus.PAID,
                    "Payment verified successfully");

            // Notify the pharmacy
//            notificationService.notifyPharmacy(claim.getProvidedService().getContractDetail().getServicelist().getProvider(), "Payment confirmed for claim " + claimUuid);

            return ResponseEntity.ok(new MessageResponse("Payment verified successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(new MessageResponse("Payment verification failed"));
        }
    }

    @Override
    public ResponseEntity<?> createBatchClaim(String batchCode) {
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();

        BatchRecord batch=batchRecordRepository.findByBatchCode(batchCode).orElseThrow(()->new BadRequestException("batch not found"));
        if (userDetails.getProviderUuid()==null)throw new BadRequestException("allowed only for provider ");
        if (!batch.getStatus().equals(Status.AUTHORIZED.toString()))throw new BadRequestException("batch is not authorized");
        List<MedicationDispensing> authorizedMedications=new ArrayList<>();
        for (MedicationDispensing medicationDispensing:batch.getMedicationDispensing()){
            medicationDispensing.setStatus(Status.AUTHORIZED);
            authorizedMedications.add(medicationDispensing);
        }
        medicationDispensingRepository.saveAll(authorizedMedications);
        Claim claim=new Claim();
        claim.setBatchRecord(batch);
        claim.setPayerUuid(batch.getPayerName());
        claimRepository.save(claim);
        return ResponseEntity.ok("claim created successfully");

    }

    @Override
    public ClaimResponse getAll(Pageable pageable) {
//        Page<ClaimResponse>claims=claimRepository.findClaimServicesByPatientId(pageable);
        return  null;
    }
}