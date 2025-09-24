package com.medco.HealthConnectProvider.services.impl.claims;

import com.medco.HealthConnectProvider.config.ClaimStatusUpdater.ClaimStatusUpdater;
import com.medco.HealthConnectProvider.config.ClaimStatusUpdater.UpdateClaimStatus;
import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.dto.MedicationDispensingDTO;
import com.medco.HealthConnectProvider.entity.claims.*;
import com.medco.HealthConnectProvider.entity.integration.FailedExternalDispensingLog;
import com.medco.HealthConnectProvider.entity.integration.MedicationDispensing;
import com.medco.HealthConnectProvider.entity.integration.MedicationDispensingItem;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.user.User;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.claims.*;
import com.medco.HealthConnectProvider.repository.contract.ContractRepository;
import com.medco.HealthConnectProvider.repository.integration.MedicationDispensingRepository;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.persons.DependantRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
import com.medco.HealthConnectProvider.repository.user.UserRepository;
import com.medco.HealthConnectProvider.services.claims.ClaimService;
import com.medco.HealthConnectProvider.services.integration.FailedExternalDispensingService;
import com.medco.HealthConnectProvider.services.notification.NotificationService;
import com.medco.HealthConnectProvider.services.payment.PaymentService;
import com.medco.HealthConnectProvider.services.providers.ProviderService;
import com.medco.HealthConnectProvider.ui.request.claims.ClaimRequest;
import com.medco.HealthConnectProvider.dto.ClaimPaySyncRequest;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;
import com.medco.HealthConnectProvider.ui.request.claims.ClaimCommentRequest;
import com.medco.HealthConnectProvider.ui.request.claims.ClaimPaymentRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.claims.*;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import com.medco.HealthConnectProvider.utils.enums.MedicationStatus;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import java.time.LocalDate;
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

    private final BatchLogRepository batchLogRepository;

    private final ProviderRepository providerRepository;
    private final PayerRepository payerRepository;
    private final ProviderService providerService;

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    private final NotificationService notificationService;
    private final BatchRecordRepository batchRecordRepository;
    private final MedicationDispensingRepository medicationDispensingRepository;
    private final ClaimStatusUpdater claimStatusUpdater;
    private final FailedExternalDispensingService failedExternalDispensingService;

    @Value("${external.api.external-api-base-url}")
    private String hostDomain;

    @Value("${api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public ClaimServiceImpl(ClaimRepository claimRepository, ClaimAttachmentRepository claimAttachmentRepository, ClaimCommentRepository claimCommentRepository, ClaimLogsRepository claimLogsRepository, ClaimPaymentRepository claimPaymentRepository, ContractRepository contractRepository, InsuredRepository insuredRepository, DependantRepository dependantRepository, BatchLogRepository batchLogRepository, ProviderRepository providerRepository, PayerRepository payerRepository, ProviderService providerService, PaymentService paymentService, UserRepository userRepository, NotificationService notificationService, BatchRecordRepository batchRecordRepository, MedicationDispensingRepository medicationDispensingRepository, ClaimStatusUpdater claimStatusUpdater, FailedExternalDispensingService failedExternalDispensingService) {
        this.claimRepository = claimRepository;
        this.claimAttachmentRepository = claimAttachmentRepository;
        this.claimCommentRepository = claimCommentRepository;
        this.claimLogsRepository = claimLogsRepository;
        this.claimPaymentRepository = claimPaymentRepository;
        this.contractRepository = contractRepository;
        this.insuredRepository = insuredRepository;
        this.dependantRepository = dependantRepository;
        this.batchLogRepository = batchLogRepository;
        this.providerRepository = providerRepository;
        this.payerRepository = payerRepository;
        this.providerService = providerService;
        this.paymentService = paymentService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.batchRecordRepository = batchRecordRepository;
        this.medicationDispensingRepository = medicationDispensingRepository;
        this.claimStatusUpdater = claimStatusUpdater;
        this.failedExternalDispensingService = failedExternalDispensingService;
    }

    @Override
    @Transactional
    public ResponseEntity<?> submitClaim(ClaimRequest claimRequest) {
        return null;
    }

    @Override
    public ClaimDetailResponse getClaimByUuid(String claimUuid) {
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String institutionUuid = userDetails.getPayerUuid();

        ClaimDetailResponse response = new ClaimDetailResponse();
        BeanUtils.copyProperties(claim, response);

        System.out.println("medication size " + claim.getBatchRecord().getMedicationDispensing().size());
        System.out.println("batch code " + claim.getBatchRecord().getBatchCode());
        response.setPayerUuid(claim.getPayerUuid());
        if (claim.getBatchRecord().getMedicationDispensing() != null) {

            Provider provider = new Provider();
            Payer payer = new Payer();
            payer = payerRepository.findByPayerUuid(claim.getPayerUuid());
            provider = providerRepository.findByProviderUuid(claim.getProviderUuid());
            response.setProviderUuid(provider.getProviderUuid());
            response.setProviderName(provider.getProviderName());
            response.setProviderCode(provider.getProviderCode());
            response.setProviderCategory(provider.getCategory());
            response.setProviderEmail(provider.getEmail());
            response.setProviderPhone(provider.getTelephone());

            response.setPayerUuid(claim.getPayerUuid());
            response.setPayerName(payer.getPayerName());
            response.setPayerCode(payer.getPayerCode());

            ResponseEntity<ByteArrayResource> providerLogo = providerService.getProviderLogo(provider.getProviderUuid());

            if (providerLogo != null && providerLogo.getBody() != null) {
                byte[] logoBytes = providerLogo.getBody().getByteArray();
                String base64Logo = Base64.getEncoder().encodeToString(logoBytes);
                response.setProviderLogo(base64Logo);

            }

        }
        if (claim.getBatchRecord().getMedicationDispensing() != null)
            response.setTotalClaims(claim.getBatchRecord().getMedicationDispensing().size());

        response.setTotalAmount(claim.getTotalAmount().doubleValue());
        response.setClaimFromDate(claim.getBatchRecord().getClaimDatingFrom());
        response.setClaimFromDate(claim.getBatchRecord().getClaimDatingTo());
        response.setAttachments(claim.getAttachments().stream()
                .map(this::mapToAttachmentResponse)
                .collect(Collectors.toList()));

        response.setComments(claim.getComments().stream()
                .sorted(Comparator.comparing(ClaimComment::getCommentDate).reversed())
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList()));

        response.setLogs(claim.getLogs().stream()
                .sorted(Comparator.comparing(ClaimLogs::getActionDate).reversed())
                .map(this::mapToLogResponse)
                .collect(Collectors.toList()));

        response.setServices(mapToServiceResponse(claim.getBatchRecord().getMedicationDispensing()));
        response.setStatus(claim.getStatus().toString());

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

    private List<ProvidedServiceResponse> mapToServiceResponse(List<MedicationDispensing> services) {
        List<ProvidedServiceResponse> providedServiceResponseList = new ArrayList<>();
        for (MedicationDispensing medicationDispensing : services) {
            ProvidedServiceResponse response = new ProvidedServiceResponse();
            BeanUtils.copyProperties(medicationDispensing, response);
            if (medicationDispensing.getInsured() != null) {
                Insured insured = medicationDispensing.getInsured();
                response.setInsuredPersonUuid(insured.getInsuredUuid());
                response.setInsuredPersonName(insured.getFirstName() + " " + insured.getFatherName());
//            response.setInsuredPersonCode(insured.get());
                response.setInsuredPersonPhone(insured.getPhone());
                response.setInsuredPersonGender(insured.getGender());
                response.setInsuredPersonUuid(insured.getInsuredUuid());
                response.setEncounterDate(medicationDispensing.getDispensingDate());
                response.setInvoiceNumber(medicationDispensing.getInvoiceNumber());

            }

            if (medicationDispensing.getDependant() != null) {
                Dependant dependant = medicationDispensing.getDependant();
                response.setDependantUuid(dependant.getDependantUuid());
                response.setDependantFullName(dependant.getFirstName());
                response.setDependantRelationship(dependant.getRelationship().toString());
            }

            response.setMedicationItems(mapMedicationItemss(medicationDispensing.getItems()));
            providedServiceResponseList.add(response);

        }
        return providedServiceResponseList;
    }

    private List<MedicationDispensingDTO.MedicationItemDTO> mapMedicationItemss(List<MedicationDispensingItem> items) {
        List<MedicationDispensingDTO.MedicationItemDTO> itemResponses = new ArrayList<>();
        for (MedicationDispensingItem item : items) {
            MedicationDispensingDTO.MedicationItemDTO response = new MedicationDispensingDTO.MedicationItemDTO();
            BeanUtils.copyProperties(item, response);
            itemResponses.add(response);
            response.setItemType(item.getItemType().toString());
        }
        return itemResponses;
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateClaimStatus(String claimUuid, ClaimStatus newStatus, String comment) {

        UpdateClaimStatus updater = claimStatusUpdater.getUpdater(newStatus);
        return updater.updateTransferStatus(claimUuid, comment);

    }

    @Override
    @Transactional
    public ResponseEntity<?> reviewClaim(String claimUuid, boolean approved, String reviewComment) {
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        ClaimStatus previousStatus = claim.getStatus();
        ClaimStatus newStatus;

        boolean isProviderReview = userDetails.getProviderUuid() != null;
        boolean isPayerReview = userDetails.getPayerUuid() != null;

        if (isProviderReview) {
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
                newStatus = ClaimStatus.UNDER_REVIEW;
            } else {
                claim.setApprovedByProviderStatus("Rejected");
                newStatus = ClaimStatus.REJECTED;
            }
        } else if (isPayerReview) {
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

        claim.setStatus(ClaimStatus.valueOf(newStatus.toString()));

        claimRepository.save(claim);

        String logComment = approved ? "Claim approved" : "Claim rejected";
        if (reviewComment != null && !reviewComment.isEmpty()) {
            logComment += ": " + reviewComment;
        }

        createClaimLog(claim, userDetails, previousStatus, newStatus, logComment);

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

        if (userDetails.getProviderUuid() != null && !claim.getProviderUuid().equals(userDetails.getPayerUuid())) {
            throw new BadRequestException("You don't have access to add attachments to this claim");
        }

        if (userDetails.getPayerUuid() != null && !claim.getPayerUuid().equals(userDetails.getPayerUuid())) {
            throw new BadRequestException("You don't have access to add attachments to this claim");
        }

        if (file.isEmpty()) {
            throw new BadRequestException("File cannot be empty");
        }

        String fileExtension = getFileExtension(file.getOriginalFilename());
        if (!isValidFileExtension(fileExtension)) {
            throw new BadRequestException("Invalid file type. Allowed types: pdf, jpg, jpeg, png, doc, docx, xls, xlsx");
        }

        try {
            File directory = new File(uploadDir + "/" + claimUuid);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;
            String filePath = uploadDir + "/" + claimUuid + "/" + uniqueFilename;

            File dest = new File(filePath);
            file.transferTo(dest);

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

        Claim claim = attachment.getClaim();
        if (userDetails.getProviderUuid() != null && !claim.getProviderUuid().equals(userDetails.getPayerUuid())) {
            throw new BadRequestException("You don't have access to delete this attachment");
        }

        if (userDetails.getPayerUuid() != null && !claim.getPayerUuid().equals(userDetails.getPayerUuid())) {
            throw new BadRequestException("You don't have access to delete this attachment");
        }

        if (claim.getStatus().equals(ClaimStatus.PAID.toString()) ||
                claim.getStatus().equals(ClaimStatus.REJECTED.toString())) {
            throw new BadRequestException("Cannot delete attachments for claims that are paid or rejected");
        }

        try {
            String filePath = uploadDir + attachment.getFileUrl().replace("/claims/attachments", "");
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }

            claimAttachmentRepository.delete(attachment);

            createClaimLog(claim, userDetails, claim.getStatus(), claim.getStatus(),
                    "Attachment deleted: " + attachment.getFileName());

            return ResponseEntity.ok(new MessageResponse("Attachment deleted successfully"));

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }

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

        if (userDetails.getProviderUuid() != null && !claim.getProviderUuid().equals(userDetails.getPayerUuid())) {
            throw new BadRequestException("You don't have access to add comments to this claim");
        }

        if (userDetails.getPayerUuid() != null && !claim.getPayerUuid().equals(userDetails.getPayerUuid())) {
            throw new BadRequestException("You don't have access to add comments to this claim");
        }

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

        createClaimLog(claim, userDetails, claim.getStatus(), claim.getStatus(),
                "Comment added: " + commentRequest.getComment().substring(0, Math.min(50, commentRequest.getComment().length())) +
                        (commentRequest.getComment().length() > 50 ? "..." : ""));

        return ResponseEntity.ok(new MessageResponse("Comment added successfully"));
    }


    @Override
    @Transactional
    public ResponseEntity<?> requestPayment(String claimUuid, String comment) {

        UpdateClaimStatus updater = claimStatusUpdater.getUpdater(ClaimStatus.PAYMENT_REQUESTED);

        return updater.updateTransferStatus(claimUuid, comment);

    }

    @Override
    public List<?> getClaimLogs(String claimUuid, Pageable pageable) {
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();

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

        if (userDetails.getPayerUuid() == null || !claim.getPayerUuid().equals(userDetails.getPayerUuid())) {
            throw new BadRequestException("Only payer users can process payments for claims");
        }

        if (!claim.getStatus().equals(ClaimStatus.APPROVED)) {
            throw new BadRequestException("Only approved claims can be processed for payment");
        }

        String paymentResponse = String.valueOf(paymentService.initiatePayment(claimUuid, BigDecimal.valueOf(paymentRequest.getAmount()), "ETB"));

        ClaimPayment payment = new ClaimPayment();
        payment.setPaymentUuid(UUID.randomUUID().toString());
        payment.setClaim(claim);
        payment.setAmount(paymentRequest.getAmount());
        payment.setPaymentType("CHAPA");
        payment.setTransactionNumber(paymentResponse);
        payment.setPaymentDate(Date.from(Instant.now()));
        payment.setPaidByUuid(userDetails.getUserUuid());
        payment.setPaidByName(userDetails.getFirstName() + " " + userDetails.getFatherName());

        claimPaymentRepository.save(payment);

        ClaimStatus previousStatus = claim.getStatus();
        claim.setStatus(ClaimStatus.PAYMENT_INITIATED);
        claim.setPaidStatus("Payment Initiated");
        claim.setPaidDate(LocalDateTime.from(Instant.now()));
        claim.setPaidByPayerUuid(userDetails.getUserUuid());
        claim.setPaidByPayerName(userDetails.getFirstName() + " " + userDetails.getFatherName());
        claim.setPaymentCode(payment.getPaymentUuid());
        claim.setTransactionNumber(paymentResponse);

        claimRepository.save(claim);

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

        if (!claim.getStatus().equals(ClaimStatus.PAYMENT_INITIATED)) {
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

    @Transactional
    @Override
    public ResponseEntity<?> createBatchClaim(String batchCode, String payerName) {
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();

        BatchRecord batch = batchRecordRepository.findByBatchCode(batchCode)
                .orElseThrow(() -> new BadRequestException("Batch not found"));

        if (userDetails.getProviderUuid() == null) {
            throw new BadRequestException("Allowed only for provider");
        }

        if (!batch.getStatus().equals(Status.SUBMITTED.toString()) && !batch.getStatus().equals(Status.RESUBMITTED.toString())) {
            throw new BadRequestException("Batch is neither submitted nor resubmitted");
        }

        List<MedicationDispensing> authorizedMedications = new ArrayList<>();

        for (MedicationDispensing medicationDispensing : batch.getMedicationDispensing()) {
            medicationDispensing.setStatus(MedicationStatus.AUTHORIZED);
            medicationDispensing.setClaimStatus(ClaimStatus.DRAFT.toString());
            authorizedMedications.add(medicationDispensing);
        }

        double totalClaimAmount = authorizedMedications.stream().mapToDouble(MedicationDispensing::getTotalAmount).sum();
        User user = userRepository.findByUserUuid(userDetails.getUserUuid())
                .orElseThrow(() -> new BadRequestException("You are not identified. Please login again"));

        medicationDispensingRepository.saveAll(authorizedMedications);

        Claim claim = new Claim();
        claim.setBatchRecord(batch);
        claim.setClaimType("CLAIM");
        claim.setMrnNumber("2");
        claim.setServiceDate(LocalDate.now());
        claim.setStatus(ClaimStatus.DRAFT);
        claim.setSubmittedByUuid(userDetails.getUserUuid());
        claim.setSubmittedByName(user.getFirstName() + " " + userDetails.getFatherName());
        claim.setTotalAmount(BigDecimal.valueOf(totalClaimAmount));
        claim.setVisitDate(LocalDateTime.now());
        claim.setPayerUuid(batch.getMedicationDispensing().get(0).getPayerUuid());

        if (userDetails.getProviderUuid() == null) {
            throw new BadRequestException("ProviderUuid not found");
        }
        claim.setProviderUuid(userDetails.getProviderUuid());

        Claim savedClaim = claimRepository.save(claim);
        batch.setStatus(ClaimStatus.APPROVED.toString());
        batchRecordRepository.save(batch);

        createClaimLog(savedClaim, userDetails, ClaimStatus.DRAFT, ClaimStatus.DRAFT, "Creating new claim status");

        try {
            Payer payer = payerRepository.findByPayerUuid(savedClaim.getPayerUuid());
            if (payer != null && payer.isInsurance()) {
                syncClaimToExternalSystem(savedClaim, batch);
            }
        } catch (Exception e) {
            System.out.println("[External Claim Sync] Failed to sync claim: " + e.getMessage());
        }

        return ResponseEntity.ok("Claim created successfully");
    }

    @Override
    public PagedResponse<ClaimListResponse> getAll(String provider, String payer, ClaimStatus status, List<ClaimStatus> statuses, Pageable pageable) {
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();
        String providerUuid = userDetails.getProviderUuid();

        Page<ClaimCustomResponse> claims = new PageImpl<>(new ArrayList<>());

        if (payerUuid != null) {
            if (provider != null) {
                claims = statuses == null ?
                        claimRepository.findAllPayerProviderClaims(payerUuid, provider, pageable) :
                        claimRepository.findAllPayerProviderClaimsByStatusIn(payerUuid, provider, statuses, pageable);
            } else {
                claims = statuses == null ?
                        claimRepository.findAllPayerClaims(payerUuid, pageable) :
                        claimRepository.findAllPayerClaimsByStatusIn(payerUuid, statuses, pageable);
            }
        } else if (providerUuid != null) {
            if (payer != null) {
                claims = statuses == null ?
                        claimRepository.findAllPayerProviderClaims(payer, providerUuid, pageable) :
                        claimRepository.findAllPayerProviderClaimsByStatusIn(payer, providerUuid, statuses, pageable);
            } else {
                claims = statuses == null ?
                        claimRepository.findAllProviderClaims(providerUuid, pageable) :
                        claimRepository.findAllProviderClaimsByStatusIn(providerUuid, statuses, pageable);
            }
        }

        List<ClaimListResponse> responseList = claims.stream().map(claimCustomResponse -> {
            ClaimListResponse claimListResponse = new ClaimListResponse();
            BeanUtils.copyProperties(claimCustomResponse, claimListResponse);
            Provider provider1 = providerRepository.findByProviderUuid(claimCustomResponse.getProviderUuid());
            claimListResponse.setProviderName(provider1.getProviderName());

            if (claimCustomResponse.getPayerUuid() != null) {
                Payer payer1 = payerRepository.findByPayerUuid(claimCustomResponse.getPayerUuid());
                claimListResponse.setPayerName(payer1 != null ? payer1.getPayerName() : null);
            }

            return claimListResponse;
        }).toList();

        return new PagedResponse<>(
                responseList,
                pageable.getPageNumber() + 1,
                pageable.getPageSize(),
                claims.getTotalElements(),
                claims.getTotalPages(),
                claims.isLast()
        );
    }

    @Override
    public ResponseEntity<?> rejectOrResubmitBatch(String batchCode, String remark) {
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();

        BatchRecord batch = batchRecordRepository.findByBatchCode(batchCode)
                .orElseThrow(() -> new RuntimeException("Batch not found with code: " + batchCode));

        Status previousStatus = Status.valueOf(batch.getStatus());
        Status newStatus;

        if (previousStatus == Status.SUBMITTED || previousStatus == Status.RESUBMITTED) {
            newStatus = Status.REJECTED;
            batch.setRejectionRemark(remark);
            batch.setRejectedBy(userDetails.getUserUuid());
            batch.setRejectedAt(LocalDateTime.now());
        } else if (previousStatus == Status.REJECTED) {
            newStatus = Status.RESUBMITTED;
            batch.setResubmissionRemark(remark);
            batch.setResubmittedBy(userDetails.getUserUuid());
            batch.setResubmittedAt(LocalDateTime.now());
        } else {
            throw new BadRequestException("Batch is not in SUBMITTED, RESUBMITTED, or REJECTED status");
        }

        batch.setStatus(newStatus.toString());
        batchRecordRepository.save(batch);

        Claim associatedClaim = claimRepository.findByBatchRecord(batch);
        if (associatedClaim != null) {
            associatedClaim.setStatus(newStatus == Status.REJECTED ? ClaimStatus.REJECTED : ClaimStatus.RESUBMITTED);
            associatedClaim.setReviewComment(remark);
            associatedClaim.setReviewedByUuid(userDetails.getUserUuid());
            associatedClaim.setReviewedAt(Instant.now());
            claimRepository.save(associatedClaim);
        }

        List<MedicationDispensing> medicationDispensings = batch.getMedicationDispensing();
        for (MedicationDispensing medicationDispensing : medicationDispensings) {
            medicationDispensing.setStatus(newStatus == Status.REJECTED ? MedicationStatus.REJECTED : MedicationStatus.RESUBMITTED);
            medicationDispensing.setClaimStatus(newStatus == Status.REJECTED ? ClaimStatus.REJECTED.toString() : ClaimStatus.RESUBMITTED.toString());
            medicationDispensing.setRemark(remark);
        }
        medicationDispensingRepository.saveAll(medicationDispensings);

        createBatchLog(batch, userDetails, previousStatus, newStatus, remark);

        return ResponseEntity.ok(newStatus == Status.REJECTED ? "Batch rejected successfully" : "Batch resubmitted successfully");
    }

    private void createBatchLog(BatchRecord batch, UserPrincipal userDetails, Status oldStatus, Status newStatus, String remark) {
        BatchLog log = new BatchLog();
        log.setBatchRecord(batch);
        log.setBatchCode(batch.getBatchCode());
        log.setOldStatus(oldStatus.toString());
        log.setNewStatus(newStatus.toString());
        log.setChangedByUuid(userDetails.getUserUuid());
        log.setChangedByName(userDetails.getFirstName() + " " + userDetails.getFatherName());
        log.setMessage("Batch rejected: " + remark);
        log.setChangedAt(LocalDateTime.now());

        batchLogRepository.save(log);
    }

    private void syncClaimToExternalSystem(Claim claim, BatchRecord batch) {
        // Filter dispensing records to only include those that have been successfully sent to external system (COMPLETED status)
        List<MedicationDispensing> filteredDispensing = batch.getMedicationDispensing().stream()
                .filter(dispensing -> {
                    // Check if this dispensing has been successfully sent (COMPLETED status)
                    String dispensingUuid = dispensing.getDispensingUuid();
                    if (dispensingUuid != null) {
                        boolean isCompleted = failedExternalDispensingService.isDispensingUuidAlreadySentSuccessfully(dispensingUuid);
                        if (!isCompleted) {
                            // Add detailed logging to diagnose the issue
                            List<FailedExternalDispensingLog> logs = failedExternalDispensingService.getFailedLogsByDispensingUuid(dispensingUuid);
                            if (logs.isEmpty()) {
                                System.out.println("[External Claim Sync] Skipping dispensing " + dispensingUuid +
                                                 " - NEVER SENT to external system (no log entry found)");
                            } else {
                                FailedExternalDispensingLog latestLog = logs.get(0); // Assuming sorted by latest
                                System.out.println("[External Claim Sync] Skipping dispensing " + dispensingUuid +
                                                 " - Status: " + latestLog.getStatus() +
                                                 ", Retry Count: " + latestLog.getRetryCount() +
                                                 ", Last Error: " + (latestLog.getErrorMessage() != null ?
                                                     latestLog.getErrorMessage().substring(0, Math.min(100, latestLog.getErrorMessage().length())) : "none"));
                            }
                        } else {
                            System.out.println("[External Claim Sync] Including dispensing " + dispensingUuid + " - COMPLETED status found");
                        }
                        return isCompleted;
                    }
                    System.out.println("[External Claim Sync] Skipping dispensing - no dispensingUuid found");
                    return false;
                })
                .toList();

        if (filteredDispensing.isEmpty()) {
            System.out.println("[External Claim Sync] No dispensing records have been successfully sent to external system yet. Skipping claim sync.");
            return;
        }

        List<String> dispensingUuids = filteredDispensing.stream()
                .map(MedicationDispensing::getDispensingUuid)
                .filter(Objects::nonNull)
                .toList();

        String contractUuid = resolveContractHeaderUuid(claim.getProviderUuid(), claim.getPayerUuid());

        // Recalculate total amount based on filtered dispensing records
        double filteredTotalAmount = filteredDispensing.stream()
                .mapToDouble(MedicationDispensing::getTotalAmount)
                .sum();

        ClaimPaySyncRequest payload = ClaimPaySyncRequest.builder()
                .contractUuid(contractUuid)
                .providerUuid(claim.getProviderUuid())
                .claimFromDate(java.sql.Date.valueOf(batch.getClaimDatingFrom()))
                .claimToDate(java.sql.Date.valueOf(batch.getClaimDatingTo()))
                .totalAmount(filteredTotalAmount)
                .batchCode(batch.getBatchCode())
                .serviceProvidedUuid(dispensingUuids)
                .build();

        String url = UriComponentsBuilder.fromHttpUrl(hostDomain)
                .path("/api/payer/claimconnect/claim/sync-claim/provider/")
                .path(claim.getClaimUuid())
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", apiKey);

        HttpEntity<ClaimPaySyncRequest> entity = new HttpEntity<>(payload, headers);

        try {
            System.out.println("[External Claim Sync] Sending claim for " + filteredDispensing.size() + " successfully completed dispensing records to external system");
            restTemplate.postForEntity(url, entity, String.class);
        } catch (RestClientException ex) {
            throw new RuntimeException("External claim sync failed: " + ex.getMessage(), ex);
        }
    }



    private String resolveContractHeaderUuid(String providerUuid, String payerUuid) {
        List<ContractHeader> activeContracts = contractRepository
                .findActiveContractsBetweenProviderAndPayer(providerUuid, payerUuid, Status.ACTIVE);
        if (activeContracts != null && !activeContracts.isEmpty()) {
            return activeContracts.get(0).getContractHeaderUuid();
        }
        throw new BadRequestException("No active contract exists between this provider and payer");
    }
}