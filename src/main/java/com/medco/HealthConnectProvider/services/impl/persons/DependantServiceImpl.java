package com.medco.HealthConnectProvider.services.impl.persons;

import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.persons.DependantRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.services.persons.DependantService;
import com.medco.HealthConnectProvider.ui.request.auth.password.persons.DependantRequest;
import com.medco.HealthConnectProvider.ui.request.persons.DependantUpdateRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.persons.DependantResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DependantServiceImpl implements DependantService {

    private static final Logger logger = LoggerFactory.getLogger(DependantServiceImpl.class);

    @Value("${file.upload-dir-payer-logos:C:/Users/Administrator/OneDrive/Desktop/MedcoProjects/logos/payers}")
    private String uploadDirectory;

    private final DependantRepository dependantRepository;
    private final InsuredRepository insuredRepository;

    public DependantServiceImpl(DependantRepository dependantRepository, InsuredRepository insuredRepository) {
        this.dependantRepository = dependantRepository;
        this.insuredRepository = insuredRepository;
    }

    @Override
    @Transactional
    public ResponseEntity<?> createDependant(DependantRequest dependantRequest, MultipartFile photo) {
        logger.info("Creating new dependant for insured person with UUID: {}", dependantRequest.getInsuredPersonUuid());

        Insured insured = insuredRepository.findByInsuredUuid(dependantRequest.getInsuredPersonUuid());
        if (insured == null){
            throw new ResourceNotFoundException("Insured", "UUID", dependantRequest.getInsuredPersonUuid());
        }

        Dependant dependant = new Dependant();
        dependant.setDependantUuid(UUID.randomUUID().toString());
        updateDependantDetails(dependant, dependantRequest);
        dependant.setInsured(insured);

        if (photo != null && !photo.isEmpty()) {
            String photoPath = saveProfilePhoto(photo, dependant.getDependantUuid());
            dependant.setProfilePicturePath(photoPath);
        }

        dependant = dependantRepository.save(dependant);

        DependantResponse response = mapToDependantResponse(dependant);
        logger.info("Successfully created dependant with UUID: {}", dependant.getDependantUuid());
        return ResponseEntity.ok(response);
    }

    @Override
    public DependantResponse getDependantByUuid(String dependantUuid) {
        logger.info("Fetching dependant with UUID: {}", dependantUuid);
        Dependant dependant = dependantRepository.findByDependantUuid(dependantUuid);
        if (dependant == null){
            throw new ResourceNotFoundException("Dependant", "UUID", dependantUuid);
        }

        return mapToDependantResponse(dependant);
    }

    @Override
    public PagedResponse<DependantResponse> getDependantsByInsuredPersonUuid(String insuredPersonUuid, int page, int size) {
        logger.info("Fetching dependants for insured person with UUID: {}", insuredPersonUuid);

        Insured insured = insuredRepository.findByInsuredUuid(insuredPersonUuid);
        if (insured == null){
            throw new ResourceNotFoundException("Insured Person", "UUID", insuredPersonUuid);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Dependant> dependantsPage = dependantRepository.findByInsured(insured, pageable);

        List<DependantResponse> dependantResponses = dependantsPage.getContent().stream()
                .map(this::mapToDependantResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                dependantResponses,
                dependantsPage.getNumber(),
                dependantsPage.getSize(),
                dependantsPage.getTotalElements(),
                dependantsPage.getTotalPages(),
                dependantsPage.isLast()
        );
    }

    @Override
    @Transactional
    public DependantResponse updateDependant(String dependantUuid, DependantUpdateRequest updateRequest, MultipartFile profilePicture) {
        logger.info("Updating dependant with UUID: {}", dependantUuid);
        Dependant dependant = dependantRepository.findByDependantUuid(dependantUuid);
        if (dependant == null){
            throw  new ResourceNotFoundException("Dependant", "UUID", dependantUuid);
        }

        updateDependantFields(dependant, updateRequest);

        if (profilePicture != null && !profilePicture.isEmpty()) {
            String fileName = saveProfilePicture(profilePicture, dependantUuid);
            dependant.setProfilePicturePath(fileName);
        }

        Dependant updatedDependant = dependantRepository.save(dependant);
        return mapToDependantResponse(updatedDependant);
    }

    @Override
    @Transactional
    public void softDeleteDependant(String dependantUuid) {
        logger.info("Soft deleting dependant with UUID: {}", dependantUuid);
        Dependant dependant = dependantRepository.findByDependantUuid(dependantUuid);
        if(dependant == null){
            throw new ResourceNotFoundException("Dependant", "UUID", dependantUuid);
        }

        dependant.setDeleted(true);
        dependant.setStatus(Status.INACTIVE);
        dependant.setDeletedAt(LocalDateTime.now());

        dependantRepository.save(dependant);
        logger.info("Dependant with UUID: {} has been soft deleted", dependantUuid);
    }


    @Override
    public DependantResponse changeStatus(String dependantUuid, Status newStatus) {
        Dependant dependant = dependantRepository.findByDependantUuid(dependantUuid);
        if (dependant == null){
            throw new ResourceNotFoundException("Dependant", "UUID", dependantUuid);
        }

        dependant.setStatus(newStatus);
        Dependant updatedDependant = dependantRepository.save(dependant);
        logger.info("Dependant with UUID: {} has been changed its status", dependantUuid);

        return mapToDependantResponse(updatedDependant);
    }

    private void updateDependantFields(Dependant dependant, DependantUpdateRequest updateRequest) {
        if (updateRequest.getTitle() != null) dependant.setTitle(updateRequest.getTitle());
        if (updateRequest.getFirstName() != null) dependant.setFirstName(updateRequest.getFirstName());
        if (updateRequest.getFatherName() != null) dependant.setFatherName(updateRequest.getFatherName());
        if (updateRequest.getGrandFatherName() != null) dependant.setGrandFatherName(updateRequest.getGrandFatherName());
        if (updateRequest.getGender() != null) dependant.setGender(updateRequest.getGender());
        if (updateRequest.getBirthDate() != null) dependant.setBirthDate(updateRequest.getBirthDate());
        if (updateRequest.getRelationship() != null) dependant.setRelationship(updateRequest.getRelationship());
        if (updateRequest.getStatus() != null) dependant.setStatus(updateRequest.getStatus());
    }

    private String saveProfilePicture(MultipartFile file, String dependantUuid) {
        try {
            String fileName = dependantUuid + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path targetLocation = Paths.get(uploadDirectory).resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    private void updateDependantDetails(Dependant dependant, DependantRequest request) {
        dependant.setFirstName(request.getDependantFirstName());
        dependant.setFatherName(request.getDependantFatherName());
        dependant.setGrandFatherName(request.getDependantGrandFatherName());
        dependant.setGender(request.getDependantGender());
        dependant.setBirthDate(request.getDependantBirthDate());
        dependant.setRelationship(request.getRelationship());
        dependant.setStatus(request.getDependantStatus() != null ? request.getDependantStatus() : Status.PENDING);
    }

    private String saveProfilePhoto(MultipartFile photo, String dependantUuid) {
        try {
            String fileName = dependantUuid + "_" + System.currentTimeMillis() + "_" + photo.getOriginalFilename();
            Path uploadPath = Paths.get(uploadDirectory);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(photo.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            logger.info("Saved profile photo for dependant UUID: {}", dependantUuid);
            return fileName;
        } catch (IOException e) {
            logger.error("Could not save profile photo for dependant UUID: {}", dependantUuid, e);
            throw new RuntimeException("Could not store file. Please try again!", e);
        }
    }

    private DependantResponse mapToDependantResponse(Dependant dependant) {
        DependantResponse response = new DependantResponse();
        BeanUtils.copyProperties(dependant, response);
        response.setInsuredPersonUuid(dependant.getInsured().getInsuredUuid());

        String photoBase64 = getDependantPhotoBase64(dependant.getDependantUuid());
        if (photoBase64 != null && !photoBase64.isEmpty()) {
            String contentType = determineContentType(dependant.getProfilePicturePath());
            response.setProfilePictureBase64("data:" + contentType + ";base64," + photoBase64);
        }

        return response;
    }

    private String determineContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }

    private String getDependantPhotoBase64(String dependantUuid) {
        try {
            Dependant dependant = dependantRepository.findByDependantUuid(dependantUuid);
            if (dependant == null){
                throw new ResourceNotFoundException("Dependant", "UUID", dependantUuid);
            }

            String photoPath = dependant.getProfilePicturePath();
            if (StringUtils.hasText(photoPath)) {
                Path path = Paths.get(uploadDirectory, photoPath);
                if (Files.exists(path)) {
                    byte[] photoBytes = Files.readAllBytes(path);
                    return Base64.getEncoder().encodeToString(photoBytes);
                }
            }
            return getDefaultPhotoBase64();
        } catch (IOException e) {
            logger.error("Error fetching photo for dependant UUID: {}", dependantUuid, e);
            return getDefaultPhotoBase64();
        }
    }

    private String getDefaultPhotoBase64() {
        try {
            ClassPathResource resource = new ClassPathResource("static/default-profile.png");
            byte[] photoBytes = StreamUtils.copyToByteArray(resource.getInputStream());
            return Base64.getEncoder().encodeToString(photoBytes);
        } catch (IOException e) {
            logger.error("Error fetching default photo", e);
            return "";
        }
    }

    @Override
    public DependantResponse getDependant(String dependantUuid) {
        Dependant dependant = dependantRepository.findByDependantUuid(dependantUuid);
        DependantResponse dependantResponse = new DependantResponse();
        BeanUtils.copyProperties(dependant, dependantResponse);
        return dependantResponse;
    }

    @Override
    public List<DependantResponse> getPersonDependants(String insuredPersonUuid, Status status) {

        List<Dependant> dependantList = dependantRepository.findAllByIsDeletedAndInsuredInsuredUuidAndStatus(false,
                insuredPersonUuid, status);

        List<DependantResponse> dependantResponse = new ArrayList<>();
        for (Dependant p : dependantList) {
            DependantResponse pr = new DependantResponse();
            BeanUtils.copyProperties(p, pr);
            dependantResponse.add(pr);
        }
        return dependantResponse;
    }

    @Override
    public ResponseEntity<?> deleteDependant(String dependantUuid) {
        Dependant dependant = dependantRepository.findByDependantUuid(dependantUuid);

        if (dependant == null)
            throw new ResourceNotFoundException("Dependant", "dependantUuid", dependantUuid);
        dependant.setDeleted(true);
        dependantRepository.save(dependant);
        return ResponseEntity.ok(new MessageResponse("Dependant person deleted successfully!"));
    }

}
