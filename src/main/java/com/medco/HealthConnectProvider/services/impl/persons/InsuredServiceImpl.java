package com.medco.HealthConnectProvider.services.impl.persons;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.text.ParseException;
import java.util.Date;
import java.util.stream.Collectors;

import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.group.EmployeeDependantGroupRepository;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.persons.DependantRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.services.persons.InsuredService;
import com.medco.HealthConnectProvider.ui.request.auth.password.persons.DependantRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.persons.InsuredRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.persons.InsuredWithDependantsRequest;
import com.medco.HealthConnectProvider.ui.request.persons.InsuredUpdateRequest;
import com.medco.HealthConnectProvider.ui.response.ApiErrorResponse;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.persons.*;
import com.medco.HealthConnectProvider.utils.enums.Relationship;
import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.transaction.Transactional;


@Slf4j
@Service
public class InsuredServiceImpl implements InsuredService {

    private static final Logger logger = LoggerFactory.getLogger(InsuredServiceImpl.class);

    private final InsuredRepository insuredRepository;

    private final PayerRepository payerRepository;

    private final DependantRepository dependantRepository;

    private final EmployeeDependantGroupRepository groupRepository;

    @Value("${file.upload-dir-payer-logos:C:/Users/Administrator/OneDrive/Desktop/MedcoProjects/logos/payers}")
    private String payerLogosDirectory;

    public InsuredServiceImpl(InsuredRepository insuredRepository, PayerRepository payerRepository, DependantRepository dependantRepository, EmployeeDependantGroupRepository groupRepository) {
        this.insuredRepository = insuredRepository;
        this.payerRepository = payerRepository;
        this.dependantRepository = dependantRepository;
        this.groupRepository = groupRepository;
    }

    @Override
    public ResponseEntity<?> createInsuredPerson(InsuredRequest insuredRequest, MultipartFile photo) {

        try {

            if (insuredRepository.existsByEmailAndPayerPayerUuid(insuredRequest.getEmail(),
                    insuredRequest.getPayerUuid())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Error: Email is already in use!");
            }

            if (insuredRepository.existsByPhoneAndPayerPayerUuid(insuredRequest.getPhone(),
                    insuredRequest.getPayerUuid())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Error: Mobile Phone is already in use!");
            }

            Insured insured = new Insured();
            BeanUtils.copyProperties(insuredRequest, insured);

            insured.setStatus(insuredRequest.getStatus() != null ?
                    insuredRequest.getStatus() : Status.PENDING);

            Payer payer = payerRepository.findByPayerUuid(insuredRequest.getPayerUuid());
            if (payer == null) throw new BadRequestException("Institution not found");
            insured.setPayer(payer);

            if (insured.getInsuredUuid() == null || insured.getInsuredUuid().isEmpty()) {
                insured.setInsuredUuid(UUID.randomUUID().toString());
            }

            if (photo != null && !photo.isEmpty()) {
                try {

                    File directory = new File(payerLogosDirectory);
                    if (!directory.exists()) {
                        directory.mkdirs();
                        logger.info("Created directory: {}", payerLogosDirectory);
                    }

                    String fileName = photo.getOriginalFilename();
                    String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                    String newFileName = "photo_" + insured.getInsuredUuid() + "." + extension;

                    Path path = Paths.get(payerLogosDirectory + "/" + newFileName);
                    Files.write(path, photo.getBytes());
                    logger.info("Saved photo to: {}", path);

                    insured.setProfilePicturePath(newFileName);
                } catch (IOException e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error uploading photo: " + e.getMessage());
                }
            }

            Insured savedInsured = insuredRepository.save(insured);

            if(insuredRequest.getGroupUuid() !=null && !insuredRequest.getGroupUuid().isEmpty()){
                addInsuredToGroup(savedInsured, insuredRequest.getGroupUuid());
            }

            InsuredResponse response = new InsuredResponse();
            BeanUtils.copyProperties(savedInsured, response);
            //response.setInsuranceId(savedInsured.getInsuranceId());
            response.setEmployeeId(savedInsured.getEmployeeId());
            response.setNationalId(savedInsured.getNationalId());
            response.setProfilePicturePath(savedInsured.getProfilePicturePath());

            if (savedInsured.getProfilePicturePath() != null) {

                try {
                    Path path = Paths.get(payerLogosDirectory + "/" + savedInsured.getProfilePicturePath());
                    byte[] fileContent = Files.readAllBytes(path);
                    String base64Photo = Base64.getEncoder().encodeToString(fileContent);
                    response.setPhotoBase64(base64Photo);
                } catch (IOException e) {
                    logger.warn("Could not read photo for insured {}: {}", savedInsured.getInsuredUuid(), e.getMessage());
                }

            }

            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error creating insured person: " + e.getMessage()));
        }
    }

    private void addInsuredToGroup(Insured savedInsured, String groupUuid) {
        EmployeeDependantGroup group = groupRepository.findByGroupUuid(groupUuid);
        if (group == null){
            throw new ResourceNotFoundException("Employee group", "groupUuid", groupUuid);
        }

        savedInsured.setEmployeeDependantGroup(group);
        group.getInsureds().add(savedInsured);
        groupRepository.save(group);
    }

    private String saveProfilePhoto(MultipartFile photo, String insuredUuid) throws IOException {
        log.debug("Saving profile photo for insured UUID: {}", insuredUuid);

        File directory = new File(payerLogosDirectory);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                log.debug("Created directory: {}", payerLogosDirectory);
            } else {
                log.warn("Failed to create directory: {}", payerLogosDirectory);
            }
        }

        String fileName = photo.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            log.warn("Original filename is null or empty for insured UUID: {}", insuredUuid);
            throw new IllegalArgumentException("Invalid file name");
        }

        String extension = FilenameUtils.getExtension(fileName);
        String newFileName = insuredUuid + "." + extension;
        Path filePath = Paths.get(payerLogosDirectory, newFileName);

        log.debug("New file name: {}", newFileName);
        log.debug("Full file path: {}", filePath);

        try {

            Files.write(filePath, photo.getBytes());
            log.info("Successfully saved profile photo for insured UUID: {} at path: {}", insuredUuid, filePath);
        } catch (IOException e) {
            log.error("Failed to save profile photo for insured UUID: {}", insuredUuid, e);
            throw new IOException("Failed to save profile photo", e);
        }

        return newFileName;

    }

    @Override
    public ResponseEntity<ByteArrayResource> getInsuredPhoto(String insuredUuid) {
        try {
            Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
            if (insured == null || insured.getProfilePicturePath() == null) {
                return ResponseEntity.notFound().build();
            }

            String photoPath = payerLogosDirectory + "/insured/photos/" + insured.getProfilePicturePath();
            Path path = Paths.get(photoPath);
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(getContentType(photoPath)))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String determineContentType(String path) {
        String extension = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }

    @Override
    public ResponseEntity<?> getInsuredPersonWithPhotoBase64(String insuredUuid) {
        Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
        if (insured == null) {
            throw new ResourceNotFoundException("Insured Person", "insuredUuid", insuredUuid);
        }

        InsuredResponse response = new InsuredResponse();
        BeanUtils.copyProperties(insured, response);

        String photoBase64 = getInsuredPhotoBase64(insuredUuid);
        response.setPhotoBase64(photoBase64);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<?> getInsuredPersonByUuid(String insuredUuid) {

        try {
            Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
            if (insured == null) {
                throw new ResourceNotFoundException("Insured person", "UUID", insuredUuid);
            }

            InsuredWithDependantsResponse response = new InsuredWithDependantsResponse();
            BeanUtils.copyProperties(insured, response);

            setProfilePictureBase64(insured.getProfilePicturePath(), response);

            Payer payer = insured.getPayer();
            if (payer != null) {
                response.setDependantCoverage(payer.isDependantCoverage());
            } else {
                log.warn("Payer not found for insured with UUID: {}", insuredUuid);
                response.setDependantCoverage(false);
            }

            if (insured.getDependants() != null && !insured.getDependants().isEmpty()) {
                List<DependantResponse> dependantResponses = insured.getDependants().stream()
                        .filter(dependant -> !dependant.isDeleted())
                        .map(this::mapToDependantResponse)
                        .collect(Collectors.toList());
                response.setDependants(dependantResponses);
            }

            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching insured person: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponse("An error occurred while fetching the insured person"));
        }
    }

    private void setProfilePictureBase64(String profilePicturePath, Object response) {
        if (profilePicturePath != null) {
            try {
                Path path = Paths.get(payerLogosDirectory, profilePicturePath);
                byte[] imageBytes = Files.readAllBytes(path);
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                String contentType = determineContentType(profilePicturePath);
                String formattedBase64 = "data:" + contentType + ";base64," + base64Image;

                if (response instanceof InsuredWithDependantsResponse) {
                    ((InsuredWithDependantsResponse) response).setProfilePictureBase64(formattedBase64);
                } else if (response instanceof DependantResponse) {
                    ((DependantResponse) response).setProfilePictureBase64(formattedBase64);
                }
            } catch (IOException e) {
                log.error("Error reading profile picture: {}", e.getMessage());
                if (response instanceof InsuredWithDependantsResponse) {
                    ((InsuredWithDependantsResponse) response).setProfilePictureBase64("");
                } else if (response instanceof DependantResponse) {
                    ((DependantResponse) response).setProfilePictureBase64("");
                }
            }
        } else {
            if (response instanceof InsuredWithDependantsResponse) {
                ((InsuredWithDependantsResponse) response).setProfilePictureBase64("");
            } else if (response instanceof DependantResponse) {
                ((DependantResponse) response).setProfilePictureBase64("");
            }
        }
    }

    @Override
    public ResponseEntity<PagedResponse<InsuredWithDependantsResponse>> getAllInsuredPersonsWithDependants(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("firstName").ascending());

        Page<Insured> insuredPage;
        if (search != null && !search.trim().isEmpty()) {
            insuredPage = insuredRepository.findBySearchTerm(search.trim(), pageable);
        } else {
            insuredPage = insuredRepository.findAll(pageable);
        }

        List<InsuredWithDependantsResponse> insuredWithDependantsList = insuredPage.getContent().stream()
                .map(this::mapToInsuredWithDependantsResponse)
                .collect(Collectors.toList());

        PagedResponse<InsuredWithDependantsResponse> pagedResponse = new PagedResponse<>(
                insuredWithDependantsList,
                page,
                size,
                insuredPage.getTotalElements(),
                insuredPage.getTotalPages(),
                insuredPage.isLast()
        );

        return ResponseEntity.ok(pagedResponse);
    }

    @Override
    public ResponseEntity<PagedResponse<InsuredDependantResponse>> getAllInsuredPersonsWithDependentsByPayer(
            String payerUuid, int page, int size, String search, String contractUuid) {
        log.info("Fetching insured persons with dependents for payer UUID: {}, page: {}, size: {}, search: {}, contractUuid: {}",
                payerUuid, page, size, search, contractUuid);

        Payer payer = payerRepository.findByPayerUuid(payerUuid);
        if (payer == null) {
            throw new ResourceNotFoundException("Payer", "payerUuid", payerUuid);
        }

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("firstName").ascending());

        Page<Insured> insuredPage;
        if (StringUtils.hasText(contractUuid)) {

            if (StringUtils.hasText(search)) {
                insuredPage = insuredRepository.findByPayerAndNotInContractAndSearchKey(payerUuid, contractUuid, false, search, pageable);
            } else {
                insuredPage = insuredRepository.findByPayerAndNotInContract(payerUuid, contractUuid, false, pageable);
            }
        } else {

            if (StringUtils.hasText(search)) {
                insuredPage = insuredRepository.findByPayerAndSearchKey(payerUuid, false, search, pageable);
            } else {
                insuredPage = insuredRepository.findByPayerPayerUuidAndIsDeleted(payerUuid, false, pageable);
            }
        }

        List<InsuredDependantResponse> insuredDependantResponses = insuredPage.getContent().stream()
                .map(this::mapToInsuredDependantResponse)
                .collect(Collectors.toList());

        PagedResponse<InsuredDependantResponse> pagedResponse = new PagedResponse<>(
                insuredDependantResponses,
                page,
                size,
                insuredPage.getTotalElements(),
                insuredPage.getTotalPages(),
                insuredPage.isLast()
        );

        log.info("Successfully fetched {} insured persons for payer UUID: {}", insuredDependantResponses.size(), payerUuid);
        return ResponseEntity.ok(pagedResponse);
    }

    private InsuredDependantResponse mapToInsuredDependantResponse(Insured insured) {
        InsuredDependantResponse response = new InsuredDependantResponse();
        response.setInsuredUuid(insured.getInsuredUuid());
        response.setInsuredTitle(StringUtils.hasText(insured.getTitle()) ? insured.getTitle() : "");
        response.setFirstName(insured.getFirstName());
        response.setFatherName(insured.getFatherName());
        response.setGrandFatherName(insured.getGrandFatherName());
        response.setGender(insured.getGender());
        //response.setInsuranceId(StringUtils.hasText(insured.getInsuranceId()) ? insured.getInsuranceId() : "");
        response.setPhone(StringUtils.hasText(insured.getPhone()) ? insured.getPhone() : "");
        response.setEmail(StringUtils.hasText(insured.getEmail()) ? insured.getEmail() : "");
        response.setBirthDate(insured.getBirthDate());
        response.setStatus(insured.getStatus() != null ? insured.getStatus() : Status.PENDING);
        response.setAddress(StringUtils.hasText(insured.getAddress()) ? insured.getAddress() : "");
        response.setWoreda(insured.getWoreda());
        response.setKebelle(insured.getKebelle());
        response.setPosition(insured.getPosition());
        response.setIdNumber(insured.getIdNumber());
        response.setEmployeeId(insured.getEmployeeId());


        String photoBase64 = getInsuredPhotoBase64(insured.getInsuredUuid());
        if (StringUtils.hasText(photoBase64)) {
            String contentType = determineContentType(insured.getProfilePicturePath());
            String formattedBase64 = "data:" + contentType + ";base64," + photoBase64;
            response.setProfilePictureBase64(formattedBase64);
        } else {
            response.setProfilePictureBase64("");
        }

        List<DependantInsuredResponse> dependantResponses = dependantRepository.findByInsuredAndIsDeletedFalse(insured)
                .stream()
                .map(this::mapToDependantInsuredResponse)
                .collect(Collectors.toList());
        response.setDependants(dependantResponses);

        return response;
    }

    private DependantInsuredResponse mapToDependantInsuredResponse(Dependant dependant) {
        DependantInsuredResponse response = new DependantInsuredResponse();
        response.setInsuredPersonUuid(dependant.getInsured().getInsuredUuid());
        response.setDependantUuid(dependant.getDependantUuid());
        response.setDependantFirstName(dependant.getFirstName());
        response.setDependantFatherName(dependant.getFatherName());
        response.setDependantGrandFatherName(dependant.getGrandFatherName());
        response.setDependantGender(dependant.getGender());
        response.setDependantStatus(dependant.getStatus() != null ? dependant.getStatus() : Status.PENDING);
        response.setRelationship(dependant.getRelationship());
        response.setDependantBirthDate(dependant.getBirthDate());
        return response;
    }

    public String getInsuredPhotoBase64(String insuredUuid) {
        log.debug("Fetching photo for insured UUID: {}", insuredUuid);
        try {
            Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
            if (insured == null){
                log.warn("Insured not found for UUID: {}", insuredUuid);
                throw new ResourceNotFoundException("Insured", "UUID", insuredUuid);
            }

            String photoPath = insured.getProfilePicturePath();
            log.debug("Profile picture path for insured UUID: {}: {}", insuredUuid, photoPath);

            if (StringUtils.hasText(photoPath)) {
                Path path = Paths.get(payerLogosDirectory, photoPath);
                log.debug("Full path for photo: {}", path);
                if (Files.exists(path)) {
                    byte[] photoBytes = Files.readAllBytes(path);
                    String base64Photo = Base64.getEncoder().encodeToString(photoBytes);
                    log.debug("Successfully encoded photo for insured UUID: {}. Base64 length: {}", insuredUuid, base64Photo.length());
                    return base64Photo;
                } else {
                    log.warn("Photo file does not exist at path: {}", path);
                }
            } else {
                log.warn("No photo path set for insured UUID: {}", insuredUuid);
            }

            log.debug("Returning default photo for insured UUID: {}", insuredUuid);
            return getDefaultPhotoBase64();
        } catch (IOException e) {
            log.error("Error fetching photo for insured UUID: {}", insuredUuid, e);
            return getDefaultPhotoBase64();
        }
    }

    private String getDefaultPhotoBase64() {
        try {

            ClassPathResource resource = new ClassPathResource("static/default-profile.png");
            byte[] photoBytes = StreamUtils.copyToByteArray(resource.getInputStream());
            return Base64.getEncoder().encodeToString(photoBytes);
        } catch (IOException e) {
            log.error("Error fetching default photo", e);
            return "";
        }
    }


    @Override
    @Transactional
    public ResponseEntity<?> softDeleteInsuredPerson(String insuredUuid) {
        log.info("Attempting to soft delete insured person with UUID: {}", insuredUuid);

        Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
        if (insured == null) {
            log.warn("Insured person not found with UUID: {}", insuredUuid);
            throw new ResourceNotFoundException("Insured Person", "insuredUuid", insuredUuid);
        }

        insured.setDeleted(true);
        insured.setDeletedAt(LocalDateTime.now());
        insuredRepository.save(insured);

        List<Dependant> dependants = dependantRepository.findByInsured(insured);
        for (Dependant dependant : dependants) {
            dependant.setDeleted(true);
            dependant.setDeletedAt(LocalDateTime.now());
        }
        dependantRepository.saveAll(dependants);

        log.info("Successfully soft deleted insured person with UUID: {} and their dependants", insuredUuid);
        return ResponseEntity.ok(new MessageResponse("Insured person and their dependants have been successfully deleted."));
    }

    @Override
    public InsuredResponse updateInsuredStatus(String insuredUuid, Status newStatus) {
        log.info("Updating status for insured person with UUID: {} to {}", insuredUuid, newStatus);
        Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
        if (insured == null){
            throw new ResourceNotFoundException("Insured", "UUID", insuredUuid);
        }

        insured.setStatus(newStatus);
        Insured updatedInsured = insuredRepository.save(insured);

        if (newStatus == Status.ACTIVE || newStatus == Status.INACTIVE){
            List<Dependant> dependants = dependantRepository.findByInsured(insured);
            for (Dependant dependant : dependants){
                dependant.setStatus(newStatus);
            }
            dependantRepository.saveAll(dependants);

            log.info("Insured person with UUID: {} and their dependants have been updated to status: {}",
                    insuredUuid, newStatus);
        }else {
            log.info("Insured person with UUID: {} has been updated to status: {}", insuredUuid, newStatus);
        }
        return mapToInsuredResponse(updatedInsured);
    }

    @Override
    public List<InsuredSearchResponse> searchInsuredPersons(String identifier) {

        log.info("Searching for insured person with identifier: {}", identifier);

        List<Insured> insuredList = new ArrayList<>();

        List<Insured> insuredByPhone = (List<Insured>) insuredRepository.findByPhone(identifier);
        if (!insuredByPhone.isEmpty()) {
            insuredList.addAll(insuredByPhone);
            return mapToInsuredSearchResponses(insuredList);
        }

        List<Insured> insuredByIdNumber = insuredRepository.findByIdNumber(identifier);
        if (!insuredByIdNumber.isEmpty()) {
            insuredList.addAll(insuredByIdNumber);
            return mapToInsuredSearchResponses(insuredList);
        }

       List<Insured> insuredByEmployeeId = (List<Insured>) insuredRepository.findByEmployeeId(identifier);
        if (!insuredByEmployeeId.isEmpty()) {
            insuredList.addAll(insuredByEmployeeId);
            return mapToInsuredSearchResponses(insuredList);
        }
//
//        insured = insuredRepository.findByInsuranceId(identifier);
//        if (insured != null) {
//            insuredList.add(insured);
//            return mapToInsuredSearchResponses(insuredList);
//        }

      List<Insured>  insuredByNationalId = (List<Insured>) insuredRepository.findByNationalId(identifier);
        if (!insuredByNationalId.isEmpty()) {
            insuredList.addAll(insuredByNationalId);
            return mapToInsuredSearchResponses(insuredList);
        }

        insuredList = insuredRepository.findByPhoneOrEmployeeIdOrNationalId(
                identifier, identifier, identifier);

        return mapToInsuredSearchResponses(insuredList);
    }

    private List<InsuredSearchResponse> mapToInsuredSearchResponses(List<Insured> insuredList) {
        return insuredList.stream().map(insured -> {

            InsuredSearchResponse response = new InsuredSearchResponse();
            BeanUtils.copyProperties(insured, response);
            response.setIdNumber(insured.getIdNumber());

            Payer payer = payerRepository.findByPayerUuid(insured.getPayerUuid());
            if (payer != null) {
                response.setPayerName(payer.getPayerName());
            }

            response.setInsured(isPersonCurrentlyInsured(insured));

//            String profilePicturePath = insured.getProfilePicturePath();
//            log.info("Profile picture path for insured {}: {}", insured.getInsuredUuid(), profilePicturePath);
//            String profilePictureBase64 = getProfilePictureBase64(profilePicturePath);
//            response.setProfilePictureBase64(profilePictureBase64);
//            log.info("Profile picture base64 for insured {}: {}", insured.getInsuredUuid(),
//                    profilePictureBase64 != null ? "Set" : "Null");

            if (insured.getDependants() != null && !insured.getDependants().isEmpty()) {
                List<DependantResponse> dependantResponses = insured.getDependants().stream()
                        .map(this::mapToDependantResponse)
                        .collect(Collectors.toList());
                response.setDependants(dependantResponses);
            } else {
                response.setDependants(new ArrayList<>());
            }

            return response;
        }).collect(Collectors.toList());
    }

    private String getProfilePictureBase64(String profilePicturePath) {
        if (profilePicturePath == null || profilePicturePath.isEmpty()) {
            log.warn("Profile picture path is null or empty for insured person");
            return null;
        }

        try {

            Path fullPath = Paths.get(payerLogosDirectory, profilePicturePath);
            log.info("Attempting to read profile picture from: {}", fullPath);

            if (!Files.exists(fullPath)) {
                log.warn("Profile picture file does not exist: {}", fullPath);
                return null;
            }

            byte[] fileContent = Files.readAllBytes(fullPath);
            String base64 = Base64.getEncoder().encodeToString(fileContent);

            String contentType = Files.probeContentType(fullPath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return "data:" + contentType + ";base64," + base64;
        } catch (IOException e) {
            log.error("Error reading profile picture: {}", e.getMessage());
            return null;
        }
    }

    private boolean isPersonCurrentlyInsured(Insured insured) {
        LocalDate currentDate = LocalDate.now();

        boolean isActive = insured.getStatus() == Status.ACTIVE;
        boolean isPolicyStarted = insured.getPolicyStartDate() == null || !currentDate.isBefore(insured.getPolicyStartDate());
        boolean isPolicyNotEnded = insured.getPolicyEndDate() == null || !currentDate.isAfter(insured.getPolicyEndDate());

        return isActive && isPolicyStarted && isPolicyNotEnded;
    }

    @Transactional
    @Override
    public ResponseEntity<?> updateInsuredPerson(String insuredUuid, InsuredUpdateRequest insuredRequest, MultipartFile photo) throws IOException {
        log.info("Updating insured person with UUID: {}", insuredUuid);

        Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
        if (insured == null) {
            throw new ResourceNotFoundException("Insured", "UUID", insuredUuid);
        }
        updateInsuredDetails(insured, insuredRequest);

        String photoFileName = null;
        if (photo != null && !photo.isEmpty()) {
            photoFileName = saveProfilePhoto(photo, insuredUuid);
            insured.setProfilePicturePath(photoFileName);
        }
        insured = insuredRepository.save(insured);

        InsuredResponse response = mapToInsuredResponse(insured);
        log.info("Successfully updated insured person with UUID: {}", insuredUuid);
        return ResponseEntity.ok(response);
    }

    private InsuredResponse mapToInsuredResponse(Insured insured) {
        InsuredResponse response = new InsuredResponse();
        BeanUtils.copyProperties(insured, response);
        response.setPayerUuid(insured.getPayer().getPayerUuid());

        if (insured.getProfilePicturePath() != null) {
            response.setPhotoBase64(getBase64FromPath(insured.getProfilePicturePath()));
        } else {
            response.setPhotoBase64("");
        }

        return response;
    }

    private String getBase64FromPath(String photoFileName) {
        try {
            Path path = Paths.get(payerLogosDirectory, photoFileName);
            log.debug("Attempting to read file from path: {}", path.toString());

            if (!Files.exists(path)) {
                log.error("File does not exist at path: {}", path.toString());
                return "";
            }

            byte[] imageBytes = Files.readAllBytes(path);
            log.debug("Successfully read {} bytes from file", imageBytes.length);

            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String contentType = determineContentType(photoFileName);
            String result = "data:" + contentType + ";base64," + base64Image;

            log.debug("Successfully created base64 string with length: {}", result.length());
            return result;
        } catch (IOException e) {
            log.error("Error reading profile picture: {}", e.getMessage(), e);
            return "";
        }
    }

    private void updateInsuredDetails(Insured insured, InsuredUpdateRequest request) {
        if (StringUtils.hasText(request.getTitle())) {
            insured.setTitle(request.getTitle());
        }
        if (StringUtils.hasText(request.getFirstName())) {
            insured.setFirstName(request.getFirstName());
        }
        if (StringUtils.hasText(request.getFatherName())) {
            insured.setFatherName(request.getFatherName());
        }
        if (StringUtils.hasText(request.getGrandFatherName())) {
            insured.setGrandFatherName(request.getGrandFatherName());
        }
        if (StringUtils.hasText(request.getGender())) {
            insured.setGender(request.getGender());
        }
        if (request.getBirthDate() != null) {
            insured.setBirthDate(request.getBirthDate());
        }
        if (StringUtils.hasText(request.getIdNumber())) {
            insured.setIdNumber(request.getIdNumber());
        }
        if (StringUtils.hasText(request.getPhone())) {
            insured.setPhone(request.getPhone());
        }
        if (StringUtils.hasText(request.getEmail())) {
            insured.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getBranchOffice())) {
            insured.setBranchOffice(request.getBranchOffice());
        }
        if (StringUtils.hasText(request.getPosition())) {
            insured.setPosition(request.getPosition());
        }
        if (StringUtils.hasText(request.getAddress())) {
            insured.setAddress(request.getAddress());
        }
        if (StringUtils.hasText(request.getState())) {
            insured.setState(request.getState());
        }
        if (StringUtils.hasText(request.getCountry())) {
            insured.setCountry(request.getCountry());
        }
//        if (StringUtils.hasText(request.getInsuranceId())) {
//            insured.setInsuranceId(request.getInsuranceId());
//        }
        if (request.getStatus() != null) {
            insured.setStatus(request.getStatus());
        }
        if (StringUtils.hasText(request.getPayerUuid())) {
            Payer payer = payerRepository.findByPayerUuid(request.getPayerUuid());
            if (payer == null){
                throw new ResourceNotFoundException("Payer", "UUID", request.getPayerUuid());
            }
            insured.setPayer(payer);
        }
    }


    private InsuredWithDependantsResponse mapToInsuredWithDependantsResponse(Insured insured) {
        InsuredWithDependantsResponse response = new InsuredWithDependantsResponse();
        BeanUtils.copyProperties(insured, response);

        if (insured.getProfilePicturePath() != null) {
            try {
                Path path = Paths.get(payerLogosDirectory + "/" + insured.getProfilePicturePath());
                byte[] imageBytes = Files.readAllBytes(path);
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                String contentType = determineContentType(insured.getProfilePicturePath());
                String formattedBase64 = "data:" + contentType + ";base64," + base64Image;
                response.setProfilePictureBase64(formattedBase64);
            } catch (IOException e) {

                log.error("Error reading profile picture for insured {}: {}", insured.getInsuredUuid(), e.getMessage());
                response.setProfilePictureBase64("");
            }
        } else {
            response.setProfilePictureBase64("");
        }

        if (insured.getDependants() != null && !insured.getDependants().isEmpty()) {
            List<DependantResponse> dependantResponses = insured.getDependants().stream()
                    .filter(dependant -> !dependant.isDeleted())
                    .map(this::mapToDependantResponse)
                    .collect(Collectors.toList());
            response.setDependants(dependantResponses);
        }

        return response;
    }

    private DependantResponse mapToDependantResponse(Dependant dependant) {
        DependantResponse response = new DependantResponse();
        BeanUtils.copyProperties(dependant, response);

        if (dependant.getProfilePicturePath() != null) {
            try {
                Path path = Paths.get(payerLogosDirectory + "/" + dependant.getProfilePicturePath());
                byte[] imageBytes = Files.readAllBytes(path);
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                String contentType = determineContentType(dependant.getProfilePicturePath());
                String formattedBase64 = "data:" + contentType + ";base64," + base64Image;
                response.setProfilePictureBase64(formattedBase64);
            } catch (IOException e) {
                log.error("Error reading profile picture for dependant {}: {}", dependant.getDependantUuid(), e.getMessage());
                response.setProfilePictureBase64("");
            }
        } else {
            response.setProfilePictureBase64("");
        }

        if (dependant.getInsured() != null) {
            response.setInsuredPersonUuid(dependant.getInsured().getInsuredUuid());
        }

        return response;
    }

    private PagedResponse<InsuredDependantResponse> buildPagedResponse(
            Page<Insured> insuredPage, int page, int limit) {

        List<InsuredDependantResponse> responseList = insuredPage.getContent().stream()
                .map(this::mapToInsuredDependantResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                responseList,
                page,
                limit,
                insuredPage.getTotalElements(),
                insuredPage.getTotalPages(),
                insuredPage.isLast()
        );
    }


    private String getContentType(String path) {
        String extension = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }


    @Override
    public ResponseEntity<?> deleteInsuredPerson(String insuredUuid) {
        Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
        if (insured == null)
            throw new ResourceNotFoundException("Insured Person", "insuredPersonUuid", insuredUuid);
        insured.setDeleted(true);
        insuredRepository.save(insured);
        return ResponseEntity.ok(new MessageResponse("Insured person deleted successfully!"));

    }

    @Transactional
    public ResponseEntity<?> updateInsuredPersonWithDependants(String insuredUuid, InsuredWithDependantsRequest insuredRequest, MultipartFile photo) {
        try {

            Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
            if (insured == null)
                throw new ResourceNotFoundException("Insured Person", "insuredPersonUuid", insuredUuid);

            BeanUtils.copyProperties(insuredRequest, insured, "status", "institutionUuid", "dependants");

            if (insuredRequest.getStatus() != null)
                insured.setStatus(insuredRequest.getStatus());
            else
                insured.setStatus(Status.PENDING);

            if (insuredRequest.getPayerUuid() != null && !insuredRequest.getPayerUuid().isEmpty()) {
                Payer payer = payerRepository.findByPayerUuid(insuredRequest.getPayerUuid());
                if (payer != null) {
                    insured.setPayer(payer);
                    insured.setPayerUuid(insuredRequest.getPayerUuid());
                }
            }

            if (photo != null && !photo.isEmpty()) {
                String uploadDir = payerLogosDirectory + "/insured/photos/";
                File directory = new File(uploadDir);
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                if (insured.getProfilePicturePath() != null) {
                    Path oldPath = Paths.get(uploadDir + insured.getProfilePicturePath());
                    try {
                        Files.deleteIfExists(oldPath);
                    } catch (IOException e) {

                        logger.warn("Could not delete old photo: " + e.getMessage());
                    }
                }

                String fileName = photo.getOriginalFilename();
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                String newFileName = insured.getInsuredUuid() + "." + extension;

                Path path = Paths.get(uploadDir + newFileName);
                Files.write(path, photo.getBytes());

                insured.setProfilePicturePath(Arrays.toString(newFileName.getBytes()));
            }

            insuredRepository.save(insured);

            if (insuredRequest.getDependants() != null && !insuredRequest.getDependants().isEmpty()) {
                // Rest of the dependant processing
                // ...
            }

            return ResponseEntity.ok(new MessageResponse("Insured person and dependants updated successfully!"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error processing photo: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error updating insured person: " + e.getMessage()));
        }
    }

    /**
     * Updates the properties of a dependant entity from a request
     * @param dependant The dependant entity to update
     * @param request The request containing the new values
     */
    private void updateDependantProperties(Dependant dependant, DependantRequest request) {
        if (request.getDependantFirstName() != null) {
            dependant.setFirstName(request.getDependantFirstName());
        }
        if (request.getDependantFatherName() != null) {
            dependant.setFatherName(request.getDependantFatherName());
        }
        if (request.getDependantGrandFatherName() != null) {
            dependant.setGrandFatherName(request.getDependantGrandFatherName());
        }
        if (request.getDependantGender() != null) {
            dependant.setGender(request.getDependantGender());
        }
        if (request.getDependantBirthDate() != null) {
            dependant.setBirthDate(request.getDependantBirthDate());
        }
        if (request.getRelationship() != null) {
            dependant.setRelationship(request.getRelationship());
        }

        if (request.getDependantStatus() != null) {
            dependant.setStatus(request.getDependantStatus());
        } else {
            dependant.setStatus(Status.PENDING);
        }
    }

    /**
     * Checks if a dependant request is empty (all fields null)
     * @param request The dependant request to check
     * @return true if all fields are null, false otherwise
     */
    private boolean isEmptyDependant(DependantRequest request) {
        return request.getDependantFirstName() == null &&
                request.getDependantFatherName() == null &&
                request.getDependantGrandFatherName() == null &&
                request.getDependantGender() == null &&
                request.getDependantBirthDate() == null &&
                request.getRelationship() == null &&
                request.getDependantStatus() == null;
    }

    /**
     * Checks if a new dependant request has all required fields
     * @param request The dependant request to check
     * @return true if all required fields are present, false otherwise
     */
    private boolean isValidNewDependant(DependantRequest request) {
        return request.getDependantFirstName() != null && !request.getDependantFirstName().trim().isEmpty() &&
                request.getDependantFatherName() != null && !request.getDependantFatherName().trim().isEmpty() &&
                request.getDependantGrandFatherName() != null && !request.getDependantGrandFatherName().trim().isEmpty() &&
                request.getDependantGender() != null && !request.getDependantGender().trim().isEmpty() &&
                request.getDependantBirthDate() != null &&
                request.getRelationship() != null;
    }

    @Override
    @Transactional
    public ResponseEntity<?> importInsuredPersonData(File file, String institutionUuid,
                                                     String payerInstitutionContractUuid) throws IOException {

        Workbook workbook = WorkbookFactory.create(file);
        Sheet sheet = workbook.getSheetAt(0);
        List<Insured> insuredList = new ArrayList<>();
        int i = 0;
        for (Row row : sheet) {
            if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
                continue;
            }

            Insured person = new Insured();
            if (i == 0) {

                if (!row.getCell(0).getStringCellValue().equalsIgnoreCase("Title")
                        || !row.getCell(1).getStringCellValue().equalsIgnoreCase("First Name")
                        || !row.getCell(2).getStringCellValue().equalsIgnoreCase("Father Name")
                        || !row.getCell(3).getStringCellValue().equalsIgnoreCase("Grand Father Name")
                        || !row.getCell(4).getStringCellValue().equalsIgnoreCase("Gender")
                        || !row.getCell(5).getStringCellValue().equalsIgnoreCase("Date of Birth")
                        || !row.getCell(6).getStringCellValue().equalsIgnoreCase("ID Number")
                        || !row.getCell(7).getStringCellValue().equalsIgnoreCase("Phone")
                        || !row.getCell(8).getStringCellValue().equalsIgnoreCase("Email")
                        || !row.getCell(9).getStringCellValue().equalsIgnoreCase("Branch Office")
                        || !row.getCell(10).getStringCellValue().equalsIgnoreCase("Position")
                        || !row.getCell(11).getStringCellValue().equalsIgnoreCase("Address1")
                        || !row.getCell(12).getStringCellValue().equalsIgnoreCase("Address2")
                        || !row.getCell(13).getStringCellValue().equalsIgnoreCase("Address3")
                        || !row.getCell(14).getStringCellValue().equalsIgnoreCase("State")
                        || !row.getCell(15).getStringCellValue().equalsIgnoreCase("Country")
                        || !row.getCell(16).getStringCellValue().equalsIgnoreCase("Insurance Number")
                        || !row.getCell(17).getStringCellValue().equalsIgnoreCase("Effective Date")
                        || !row.getCell(18).getStringCellValue().equalsIgnoreCase("End Date")) {

                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Error: The Excel sheet for uploading insured person of an institution should be used the format given.");

                }
            }
            else {
                if (insuredRepository.existsByEmailAndInsuranceIdAndPayerUuid(row.getCell(8).getStringCellValue(),
                        row.getCell(8).getStringCellValue(), institutionUuid)) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Error: Email is already in use!");
                }

                if (insuredRepository.existsByPhoneAndInsuranceIdAndPayerUuid(row.getCell(7).getStringCellValue(),
                        row.getCell(7).getStringCellValue(), institutionUuid)) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Error: Mobile Phone is already in use");

                }

                if (insuredRepository.existsByInsuranceIdAndPayerUuid(
                        row.getCell(16).getStringCellValue(), institutionUuid)) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Error: Insurance Number is already in use!");

                }

                person.setTitle(row.getCell(0).getStringCellValue());
                person.setFirstName(row.getCell(1).getStringCellValue());
                person.setFatherName(row.getCell(2).getStringCellValue());
                person.setGrandFatherName(row.getCell(3).getStringCellValue());
                person.setGender(row.getCell(4).getStringCellValue());
                person.setBirthDate(row.getCell(5).getDateCellValue());
                person.setIdNumber(row.getCell(6).getStringCellValue());
                person.setPhone(row.getCell(7).getStringCellValue());
                person.setEmail(row.getCell(8).getStringCellValue());
                person.setBranchOffice(row.getCell(9).getStringCellValue());
                person.setPosition(row.getCell(10).getStringCellValue());

                person.setAddress(row.getCell(11).getStringCellValue());
                person.setState(row.getCell(14).getStringCellValue());
                person.setCountry(row.getCell(15).getStringCellValue());
               // person.setInsuranceId(row.getCell(16).getStringCellValue());

//				person.setPayerInstitutionContractUuid(payerInstitutionContractUuid);
                person.setStatus(Status.ACTIVE);
                person.setPayerUuid(institutionUuid);

                insuredList.add(person);

            }
            i++;
        }
        insuredRepository.saveAll(insuredList);

        workbook.close();

        return ResponseEntity.ok(new MessageResponse("Insured person imported successfully!"));

    }

    @Override
    public ResponseEntity<?> setProfilePicture(MultipartFile file, String insuredUuid) throws IOException {
        Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);

        String uploadDir = payerLogosDirectory;
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        List<Object> fileLists = new ArrayList<>();

        String fileName = file.getOriginalFilename();
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        String name = fileName.substring(0, fileName.lastIndexOf(".")).toLowerCase();
        String newFileName = name + "_" + insuredUuid + "." + extension;

        fileLists.add(new ByteArrayResource(file.getBytes()));
        byte[] bytes = file.getBytes();
        Path path = Paths.get(uploadDir + newFileName);
        Files.write(path, bytes);
        insured.setProfilePicturePath(Arrays.toString(newFileName.getBytes()));
        insuredRepository.save(insured);
        return ResponseEntity.ok(new MessageResponse("Profile Picture Update Successfully."));
    }

    @Override
    public List<InsuredResponse> getInsuredPersons(String payerInstitutionContractId, String search, int page,
                                                   int limit) {
        if (page > 0)
            page = page - 1;
        Pageable pageRequest = PageRequest.of(page, limit, Sort.by("id").descending());
        Page<Insured> insured;
        if (search != null)
            insured = insuredRepository
                    .findAllByIsDeletedAndFirstNameContainingOrPhoneContaining(false, search, search, pageRequest);
        else
            insured = insuredRepository.findAllByIsDeleted(false, pageRequest);

        long totalPages = insured.getTotalPages();
        List<Insured> insuredList = insured.getContent();

        List<InsuredResponse> insuredResponse = new ArrayList<>();
        for (Insured p : insuredList) {
            InsuredResponse pr = new InsuredResponse();
            if (insuredResponse.isEmpty())
                pr.setTotalPages(totalPages);
            BeanUtils.copyProperties(p, pr);
            insuredResponse.add(pr);
        }
        return insuredResponse;
    }

    @Override
    @Transactional
    public ResponseEntity<?> importInsuredPersonAndDependant(File file, String payerUuid) throws Exception, IOException {
        Workbook workbook = null;
        Logger logger = LoggerFactory.getLogger(this.getClass());
        List<String> errors = new ArrayList<>();
        List<InsuredResponse> importedInsured = new ArrayList<>();
        int successfulImports = 0;

        logger.info("Starting import process for payer UUID: {}", payerUuid);

        try {
            workbook = WorkbookFactory.create(file);
            Sheet sheet = workbook.getSheetAt(0);
            int totalRows = sheet.getPhysicalNumberOfRows();
            logger.info("Total rows in sheet: {}", totalRows);

            int numberOfColumns = 0;
            int numbrOfDependants = 0;

            logger.info("Excel file loaded successfully. Processing rows...");

            boolean headerValidated = false;
            for (int rowNum = 0; rowNum < totalRows; rowNum++) {
                Row row = sheet.getRow(rowNum);
                logger.debug("Processing row: {}", rowNum + 1);

                if (row == null || isEmptyRow(row)) {
                    logger.debug("Skipping empty row: {}", rowNum + 1);
                    continue;
                }

                if (!headerValidated) {
                    try {
                        numberOfColumns = row.getLastCellNum();
                        String[] expectedHeaders = {"ID Number", "First Name", "Father Name", "Grand Father Name", "Gender",
                                "Date of Birth", "Phone", "Email", "woreda", "subcity", "city", "state"};
                        String[] mandatoryHeaders = {"ID Number", "First Name", "Father Name"};

                        for (int i = 0; i < expectedHeaders.length; i++) {
                            String cellValue = getCellValueAsString(row.getCell(i));
                            if (!cellValue.equalsIgnoreCase(expectedHeaders[i])) {
                                logger.error("Header mismatch at column {}: Expected '{}', Found '{}'", i + 1, expectedHeaders[i], cellValue);
                                errors.add("Header mismatch at column " + (i + 1) + ": Expected '" + expectedHeaders[i] + "', Found '" + cellValue + "'");
                            }
                        }

                        if (!errors.isEmpty()) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: The Excel sheet for uploading insured person should use the standard format.");
                        }

                        if (numberOfColumns > 8) {
                            numbrOfDependants = (numberOfColumns - 8) / 6;
                            if ((numberOfColumns - 8) % 6 != 0) {
                                logger.warn("Unexpected number of columns for dependants: {}. Each dependant should have exactly 6 columns.", numberOfColumns - 8);
                            }
                        }

                        headerValidated = true;
                        logger.info("Validated header row successfully");
                        continue;
                    } catch (Exception e) {
                        logger.error("Error validating header row: {}", e.getMessage());
                        errors.add("Error validating header row: " + e.getMessage());
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: Invalid Excel format.");
                    }
                }

                try {
                    if (isEmptyOrNull(row.getCell(0)) || isEmptyOrNull(row.getCell(1)) || isEmptyOrNull(row.getCell(2))) {
                        logger.warn("Mandatory fields missing in row: {}", rowNum + 1);
                        throw new IllegalArgumentException("Mandatory fields (ID Number, First Name, Father Name) cannot be empty.");
                    }

                    InsuredResponse insuredResponse = createInsuredPerson(row, payerUuid);

                    if (insuredResponse != null) {
                        successfulImports++;
                        importedInsured.add(insuredResponse);
                        logger.info("Successfully imported insured person: {}", insuredResponse.getInsuredUuid());

                        List<Dependant> dependants = processDependants(row, insuredResponse, numbrOfDependants);
                        if (!dependants.isEmpty()) {
                            dependantRepository.saveAll(dependants);
                            logger.info("Saved {} dependants for insured person: {}", dependants.size(), insuredResponse.getInsuredUuid());
                        }
                    }
                    // If insuredResponse is null, it means it's a duplicate entry that we're skipping
                } catch (BadRequestException e) {
                    logger.error("Error processing row {}: {}", rowNum + 1, e.getMessage());
                    errors.add("Error in row " + (rowNum + 1) + ": " + e.getMessage());
                } catch (Exception e) {
                    logger.error("Unexpected error processing row {}: {}", rowNum + 1, e.getMessage());
                    errors.add("Unexpected error in row " + (rowNum + 1) + ": " + e.getMessage());
                }
            }

            logger.info("Import process completed. Successful imports: {}, Errors: {}", successfulImports, errors.size());

            InsuredImportResponse response = new InsuredImportResponse(importedInsured, errors, successfulImports);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during import process: {}", e.getMessage(), e);
            throw e;
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    logger.error("Error closing workbook: {}", e.getMessage());
                }
            }
            if (file != null && file.exists()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    logger.warn("Failed to delete temporary file: {}", file.getAbsolutePath());
                }
            }
        }
    }

    private boolean isEmptyRow(Row row) {
        for (int cellNum = 0; cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private boolean isEmptyOrNull(Cell cell) {
        return cell == null || cell.getCellType() == CellType.BLANK ||
                (cell.getCellType() == CellType.STRING && cell.getStringCellValue().trim().isEmpty());
    }

    private InsuredResponse createInsuredPerson(Row row, String payerUuid) throws BadRequestException {
        Logger logger = LoggerFactory.getLogger(this.getClass());

        logger.debug("Creating insured person from row: {}", row.getRowNum() + 1);

        String idNumber = getCellValueAsString(row.getCell(0));
        String firstName = getCellValueAsString(row.getCell(1));
        String fatherName = getCellValueAsString(row.getCell(2));
        String phone = getCellValueAsString(row.getCell(6));
        String email = getCellValueAsString(row.getCell(7));

        logger.debug("Creating insured person with ID Number: {}, First Name: {}, Father Name: {}, Phone: {}, Email: {}",
                idNumber, firstName, fatherName, phone, email);

        Insured existingPerson = insuredRepository.findByIdNumberAndFirstNameAndFatherNameAndPayerUuid(idNumber, firstName, fatherName, payerUuid);
        if (existingPerson != null) {
            logger.warn("Insured person with ID Number {}, First Name {}, and Father Name {} already exists for payer {}. Skipping this entry.",
                    idNumber, firstName, fatherName, payerUuid);
            return null;
        }

        if (insuredRepository.existsByIdNumberAndPayerUuid(idNumber, payerUuid)) {
            throw new BadRequestException("Insured person with ID Number " + idNumber + " already exists for this payer.");
        }

        if (isNotBlank(phone) && insuredRepository.existsByPhoneAndPayerUuid(phone, payerUuid)) {
            throw new BadRequestException("Insured person with phone number " + phone + " already exists for this payer.");
        }

        if (isNotBlank(email) && insuredRepository.existsByEmailAndPayerUuid(email, payerUuid)) {
            throw new BadRequestException("Insured person with email " + email + " already exists for this payer.");
        }

        Insured person = new Insured();
        person.setInsuredUuid(UUID.randomUUID().toString());

        person.setIdNumber(idNumber);
        person.setFirstName(firstName);
        person.setFatherName(fatherName);
        person.setGrandFatherName(getCellValueAsString(row.getCell(3)));
        person.setGender(getCellValueAsString(row.getCell(4)));
        person.setBirthDate(parseDateCell(row.getCell(5)));
        person.setPhone(phone);
        person.setEmail(email);

        person.setStatus(Status.ACTIVE);
        person.setPayerUuid(payerUuid);

        Payer payer = payerRepository.findByPayerUuid(payerUuid);
        if (payer == null) {
            logger.error("Institution not found with UUID: {}", payerUuid);
            throw new BadRequestException("Institution not found with UUID: " + payerUuid);
        }
        person.setPayer(payer);

        logger.debug("Saving insured person: {}", person);
        Insured savedPerson = insuredRepository.save(person);
        logger.info("Saved insured person with UUID: {}", savedPerson.getInsuredUuid());

        InsuredResponse response = InsuredResponse.builder()
                .insuredUuid(savedPerson.getInsuredUuid())
                .payerUuid(savedPerson.getPayerUuid())
                .email(savedPerson.getEmail())
                .firstName(savedPerson.getFirstName())
                .fatherName(savedPerson.getFatherName())
                .grandFatherName(savedPerson.getGrandFatherName())
                .Gender(savedPerson.getGender())
                .birthDate(savedPerson.getBirthDate())
                .phone(savedPerson.getPhone())
                .idNumber(savedPerson.getIdNumber())
                .status(savedPerson.getStatus())
                .build();

        logger.debug("Created InsuredResponse: {}", response);
        return response;
    }

    private boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    private List<Dependant> processDependants(Row row, InsuredResponse insuredResponse, int numbrOfDependants) throws BadRequestException {
        List<Dependant> dependants = new ArrayList<>();
        for (int j = 15; j < 15 + (numbrOfDependants * 6); j += 6) {
            if (j + 5 < row.getLastCellNum() && row.getCell(j) != null && !getCellValueAsString(row.getCell(j)).trim().isEmpty()) {

                Dependant dependant = new Dependant();
                dependant.setDependantUuid(UUID.randomUUID().toString());
                dependant.setFirstName(getCellValueAsString(row.getCell(j)));
                dependant.setFatherName(getCellValueAsString(row.getCell(j + 1)));
                dependant.setGrandFatherName(getCellValueAsString(row.getCell(j + 2)));
                dependant.setGender(getCellValueAsString(row.getCell(j + 3)));
                dependant.setBirthDate(parseDateCell(row.getCell(j + 4)));
                dependant.setRelationship(parseRelationship(getCellValueAsString(row.getCell(j + 5))));
                dependant.setStatus(Status.ACTIVE);

                Insured insured = insuredRepository.findByInsuredUuid(insuredResponse.getInsuredUuid());
                if (insured == null) {
                    throw new BadRequestException("Insured person not found with UUID: " + insuredResponse.getInsuredUuid());
                }
                dependant.setInsured(insured);

                dependantRepository.save(dependant);
                dependants.add(dependant);

            }
        }

        return dependants;

    }

    private Date parseDateCell(Cell cell) throws BadRequestException {
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getDateCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String dateStr = cell.getStringCellValue();
                SimpleDateFormat[] formats = {
                        new SimpleDateFormat("yyyy-MM-dd"),
                        new SimpleDateFormat("MM/dd/yyyy"),
                        new SimpleDateFormat("dd/MM/yyyy"),
                        new SimpleDateFormat("dd-MM-yyyy")
                };
                for (SimpleDateFormat format : formats) {
                    try {
                        return format.parse(dateStr);
                    } catch (ParseException e) {
                        // Try next format
                    }
                }

                throw new BadRequestException("Invalid date format: " + dateStr);

            }
        } catch (Exception e) {
            throw new BadRequestException("Error processing date: " + e.getMessage());
        }

        return null;

    }

    private Relationship parseRelationship(String relationshipStr) throws BadRequestException {
        try {
            return Relationship.valueOf(relationshipStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            for (Relationship rel : Relationship.values()) {
                if (rel.name().equalsIgnoreCase(relationshipStr)) {
                    return rel;
                }
            }
            throw new BadRequestException("Invalid relationship value: '" + relationshipStr +
                    "'. Valid values are: " + Arrays.toString(Relationship.values()));
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    return sdf.format(cell.getDateCellValue());
                }
                DecimalFormat df = new DecimalFormat("#.###");
                return df.format(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return cell.getCellFormula();
                    }
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    @Override
    public List<InsuredDependantResponse> getInsuredPersonsAndDependants(String payerInstitutionContractId,
                                                                         String search, int page, int limit) {

        if (page > 0) {
            page = page - 1;
        }

        Pageable pageable = PageRequest.of(page, limit, Sort.by(
                Sort.Order.asc("firstName"),
                Sort.Order.asc("fatherName"),
                Sort.Order.asc("grandFatherName")
        ));

        List<InsuredDependantListResponse> response = insuredRepository.findInsuredPersonsAndDependants(
                payerInstitutionContractId, pageable);

        Map<String, InsuredDependantResponse> mergedResponses = new HashMap<>();

        for (InsuredDependantListResponse item : response) {
            InsuredDependantResponse insuredDependantResponse = mergedResponses
                    .computeIfAbsent(item.getInsuredUuid(), k -> new InsuredDependantResponse());

            insuredDependantResponse.setInsuredTitle(item.getTitle());
            insuredDependantResponse.setInsuredUuid(item.getInsuredUuid());
            insuredDependantResponse.setFirstName(item.getFirstName());
            insuredDependantResponse.setFatherName(item.getFatherName());
            insuredDependantResponse.setGrandFatherName(item.getGrandFatherName());
            insuredDependantResponse.setGender(item.getGender());
            insuredDependantResponse.setInsuranceId(item.getInsuranceId());
            insuredDependantResponse.setPhone(item.getPhone());

            if (item.getDependantUuid() != null) {
                DependantInsuredResponse dependant = new DependantInsuredResponse();
                dependant.setInsuredPersonUuid(item.getInsuredUuid());
                dependant.setDependantUuid(item.getDependantUuid());
                dependant.setDependantFirstName(item.getDependantFirstName());
                dependant.setDependantFatherName(item.getDependantFatherName());
                dependant.setDependantGrandFatherName(item.getDependantGrandFatherName());
                dependant.setDependantGender(item.getDependantGender());

                try {
                    dependant.setDependantStatus(item.getDependantStatus());
                } catch (Exception e) {
                    dependant.setDependantStatus(Status.PENDING);
                }

                try {
                    dependant.setRelationship(item.getRelationship());
                } catch (Exception e) {
                    dependant.setRelationship(null);
                }

                insuredDependantResponse.getDependants().add(dependant);
            }
        }

        return new ArrayList<>(mergedResponses.values());
    }

    @Override
    public List<InsuredAndDependantCashServiceResponse> getInsuredAndDependantCashServiceResponse(
            String institutionUuid, String search, Pageable pageable) {

        Page<Insured> insureds=insuredRepository.findByPayerPayerUuid(institutionUuid,pageable);
        return insureds.stream().map(insured -> {
            InsuredAndDependantCashServiceResponse response=new InsuredAndDependantCashServiceResponse();
            BeanUtils.copyProperties(insured,response);
            response.setInsuranceId(insured.getPayer().getPayerUuid());
            response.setInsuredUuid(insured.getPayer().getPayerUuid());
            response.setInsuredUuid(insured.getPayer().getPayerUuid());
            response.setInsuredUuid(insured.getPayer().getPayerUuid());
            response.setDependants(convertToDependantResponse(insured.getDependants()));
            return response;
        }).toList();

    }

    private List<DependantCashServiceResponse> convertToDependantResponse(List<Dependant> dependents) {
        return dependents.stream().map(dependant -> {
            DependantCashServiceResponse dependantCashServiceResponse=new DependantCashServiceResponse();
            BeanUtils.copyProperties(dependant,dependantCashServiceResponse);
            return dependantCashServiceResponse;
        }).toList();
    }

    @Override
    public boolean checkMemberExist(String payerInstitutionContractUuid) {
        return insuredRepository.existsByIsDeleted( false);
    }


    private void validatePhoto(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            return;
        }

        if (photo.getSize() > 5 * 1024 * 1024) {
            throw new BadRequestException("Photo size exceeds maximum limit of 5MB");
        }

        String contentType = photo.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/gif"))) {
            throw new BadRequestException("Only JPEG, PNG, and GIF images are allowed");
        }
    }

    private String processPhoto(MultipartFile photo, String insuredUuid, String oldPhotoName) throws IOException {
        if (photo == null || photo.isEmpty()) {
            return oldPhotoName;
        }

        validatePhoto(photo);

        String uploadDir = payerLogosDirectory + "/insured/photos/";
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        if (oldPhotoName != null) {
            Path oldPath = Paths.get(uploadDir + oldPhotoName);
            try {
                Files.deleteIfExists(oldPath);
            } catch (IOException e) {
                logger.warn("Could not delete old photo: " + e.getMessage());
            }
        }

        String fileName = photo.getOriginalFilename();
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        String newFileName = insuredUuid + "." + extension;

        Path path = Paths.get(uploadDir + newFileName);
        Files.write(path, photo.getBytes());

        return newFileName;
    }

}
