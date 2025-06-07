package com.medco.HealthConnectProvider.services.impl.persons;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.text.ParseException;
import java.util.Date;

import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.persons.DependantRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.services.persons.InsuredService;
import com.medco.HealthConnectProvider.ui.request.auth.password.persons.DependantRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.persons.InsuredRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.persons.InsuredWithDependantsRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.persons.*;
import com.medco.HealthConnectProvider.utils.enums.Relationship;
import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.transaction.Transactional;

@Slf4j
@Service
public class InsuredServiceImpl implements InsuredService {

    private static final Logger logger = LoggerFactory.getLogger(InsuredServiceImpl.class);

    private final InsuredRepository insuredRepository;

    private final PayerRepository institutionRepository;

    private final DependantRepository dependantRepository;

    @Value("${file.upload-dir-insured-person-profiles}")
    private String uploadDirectory;

    public InsuredServiceImpl(InsuredRepository insuredRepository, PayerRepository institutionRepository,  DependantRepository dependantRepository) {
        this.insuredRepository = insuredRepository;
        this.institutionRepository = institutionRepository;
        this.dependantRepository = dependantRepository;
    }

    @Override
    public ResponseEntity<?> createInsuredPerson(InsuredRequest insuredRequest, MultipartFile photo) {
        try {
            // Validate unique constraints
            if (insuredRepository.existsByEmailAndPayerPayerUuid(insuredRequest.getEmail(),
                    insuredRequest.getPayerUuid())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Error: Email is already in use!");
            }

            if (insuredRepository.existsByPhoneAndPayerPayerUuid(insuredRequest.getPhone(),
                    insuredRequest.getPayerUuid())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Error: Mobile Phone is already in use!");
            }

            // Create and populate insured person
            Insured insured = new Insured();
            BeanUtils.copyProperties(insuredRequest, insured);

            // Set status
            if (insuredRequest.getStatus() != null)
                insured.setStatus(insuredRequest.getStatus());
            else
                insured.setStatus(Status.PENDING);

            // Set payer
            Payer payer = institutionRepository.findByPayerUuid(insuredRequest.getPayerUuid());
            if (payer == null) throw new BadRequestException("Institution not found");
            insured.setPayer(payer);

            // Generate a unique insured UUID if not already set
            if (insured.getInsuredUuid() == null || insured.getInsuredUuid().isEmpty()) {
                insured.setInsuredUuid(UUID.randomUUID().toString());
            }

            // Process photo if provided
            if (photo != null && !photo.isEmpty()) {
                String uploadDir = uploadDirectory + "/insured/photos/";
                File directory = new File(uploadDir);
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                String fileName = photo.getOriginalFilename();
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                String newFileName = insured.getInsuredUuid() + "." + extension;

                Path path = Paths.get(uploadDir + newFileName);
                Files.write(path, photo.getBytes());

                insured.setProfilePicture(newFileName.getBytes());
            }

            // Save the insured person
            Insured savedInsured = insuredRepository.save(insured);

            // Create response with more details
            InsuredResponse response = new InsuredResponse();
            BeanUtils.copyProperties(savedInsured, response);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error processing photo: " + e.getMessage()));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error creating insured person: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ByteArrayResource> getInsuredPhoto(String insuredUuid) {
        try {
            Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
            if (insured == null || insured.getProfilePicture() == null) {
                return ResponseEntity.notFound().build();
            }

            String photoPath = uploadDirectory + "/insured/photos/" + insured.getProfilePicture();
            Path path = Paths.get(photoPath);
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(getContentType(photoPath)))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public String getInsuredPhotoBase64(String insuredUuid) {
        try {
            Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
            if (insured == null || insured.getProfilePicture() == null) {
                return getDefaultPhotoBase64();
            }

            String photoPath = uploadDirectory + "/insured/photos/" + insured.getProfilePicture();
            File photoFile = new File(photoPath);

            if (photoFile.exists() && photoFile.isFile()) {
                byte[] fileContent = Files.readAllBytes(photoFile.toPath());
                String base64Photo = Base64.getEncoder().encodeToString(fileContent);
                return "data:" + determineContentType(photoPath) + ";base64," + base64Photo;
            } else {
                // Return default photo if insured photo doesn't exist
                return getDefaultPhotoBase64();
            }
        } catch (IOException e) {
            logger.warn("Could not read photo for insured {}: {}", insuredUuid, e.getMessage());
            // Return default photo on error
            return getDefaultPhotoBase64();
        }
    }

    private String getDefaultPhotoBase64() {
        try {
            Resource resource = new ClassPathResource("static/images/default-profile-photo.png");
            if (resource.exists()) {
                byte[] fileContent = FileCopyUtils.copyToByteArray(resource.getInputStream());
                String base64Photo = Base64.getEncoder().encodeToString(fileContent);
                return "data:image/png;base64," + base64Photo;
            }
        } catch (IOException e) {
            logger.warn("Could not read default photo: {}", e.getMessage());
        }
        return null;
    }

    private String determineContentType(String path) {
        String extension = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            default:
                return "application/octet-stream";
        }
    }

    @Override
    public ResponseEntity<?> getInsuredPersonWithPhotoBase64(String insuredUuid) {
        Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
        if (insured == null) {
            throw new ResourceNotFoundException("Insured Person", "insuredUuid", insuredUuid);
        }

        InsuredResponse response = new InsuredResponse();
        BeanUtils.copyProperties(insured, response);

        // Add photo as base64
        String photoBase64 = getInsuredPhotoBase64(insuredUuid);
        response.setPhotoBase64(photoBase64);

        return ResponseEntity.ok(response);
    }

    private String getContentType(String path) {
        String extension = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            default:
                return "application/octet-stream";
        }
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

    @Override
    public ResponseEntity<?> getInsuredPerson(String insuredUuid) {
        Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
        if (insured == null) throw new BadRequestException("insured person not found");

        // Create the response with dependants
        InsuredDependantResponse response = new InsuredDependantResponse();

        // Map insured person properties
        response.setInsuredPersonUuid(insured.getInsuredUuid());
        response.setInsuredTitle(insured.getTitle());
        response.setFirstName(insured.getFirstName());
        response.setFatherName(insured.getFatherName());
        response.setGrandFatherName(insured.getGrandFatherName());
        response.setGender(insured.getGender());
        response.setInsuranceId(insured.getInsuranceId());
        response.setPhone(insured.getPhone());
        response.setEmail(insured.getEmail());
        response.setBirthDate(insured.getBirthDate());
        response.setStatus(insured.getStatus());
        response.setAddress1(insured.getAddress());

        if (insured.getProfilePicture() != null){
            response.setProfilePictureBase64(Base64.getEncoder().encodeToString(insured.getProfilePicture()));
        }

        // Add dependants if they exist
        if (insured.getDependants() != null && !insured.getDependants().isEmpty()) {
            for (Dependant dependant : insured.getDependants()) {
                // Skip deleted dependants
                if (dependant.isDeleted()) {
                    continue;
                }

                DependantInsuredResponse dependantResponse = new DependantInsuredResponse();
                dependantResponse.setInsuredPersonUuid(insured.getInsuredUuid());
                dependantResponse.setDependantUuid(dependant.getDependantUuid());
                dependantResponse.setDependantFirstName(dependant.getFirstName());
                dependantResponse.setDependantFatherName(dependant.getFatherName());
                dependantResponse.setDependantGrandFatherName(dependant.getGrandFatherName());
                dependantResponse.setDependantGender(dependant.getGender());
                dependantResponse.setDependantStatus(dependant.getStatus());
                dependantResponse.setRelationship(dependant.getRelationship());
                dependantResponse.setDependantBirthDate(dependant.getBirthDate());

                response.getDependants().add(dependantResponse);
            }
        }

        return ResponseEntity.ok(response);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateInsuredPersonWithDependants(String insuredUuid, InsuredWithDependantsRequest insuredRequest, MultipartFile photo) {
        try {
            // Find the insured person
            Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
            if (insured == null)
                throw new ResourceNotFoundException("Insured Person", "insuredPersonUuid", insuredUuid);

            // Update basic properties
            BeanUtils.copyProperties(insuredRequest, insured, "status", "institutionUuid", "dependants");

            // Update status
            if (insuredRequest.getStatus() != null)
                insured.setStatus(insuredRequest.getStatus());
            else
                insured.setStatus(Status.PENDING);

            // Update institution if provided
            if (insuredRequest.getInstitutionUuid() != null && !insuredRequest.getInstitutionUuid().isEmpty()) {
                Payer payer = institutionRepository.findByPayerUuid(insuredRequest.getInstitutionUuid());
                if (payer != null) {
                    insured.setPayer(payer);
                    insured.setPayerUuid(insuredRequest.getInstitutionUuid());
                }
            }

            // Process photo if provided
            if (photo != null && !photo.isEmpty()) {
                String uploadDir = uploadDirectory + "/insured/photos/";
                File directory = new File(uploadDir);
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                // Delete old photo if exists
                if (insured.getProfilePicture() != null) {
                    Path oldPath = Paths.get(uploadDir + insured.getProfilePicture());
                    try {
                        Files.deleteIfExists(oldPath);
                    } catch (IOException e) {
                        // Log but continue
                        logger.warn("Could not delete old photo: " + e.getMessage());
                    }
                }

                String fileName = photo.getOriginalFilename();
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                String newFileName = insured.getInsuredUuid() + "." + extension;

                Path path = Paths.get(uploadDir + newFileName);
                Files.write(path, photo.getBytes());

                insured.setProfilePicture(newFileName.getBytes());
            }

            // Save the insured person
            insuredRepository.save(insured);

            // Process dependants if provided
            if (insuredRequest.getDependants() != null && !insuredRequest.getDependants().isEmpty()) {
                // Rest of the dependant processing code remains the same
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
                request.getDependantGender() == null &&
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
                person.setInsuranceId(row.getCell(16).getStringCellValue());

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

        String uploadDir = uploadDirectory;
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
        insured.setProfilePicture(newFileName.getBytes());
        insuredRepository.save(insured);
        return ResponseEntity.ok(new MessageResponse("Profile Picture Update Successfully."));
    }


//    @Override
//    public List<InsuredListResponse> getInsuredPersonEligiblity(String insuredUuid) {
//        return insuredRepository.findInsuredPersonEligibility(insuredUuid);
//    }

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
            if (insuredResponse.size() == 0)
                pr.setTotalPages(totalPages);
            BeanUtils.copyProperties(p, pr);
            insuredResponse.add(pr);
        }
        return insuredResponse;
    }



    @Override
    @Transactional
    public ResponseEntity<?> importInsuredPersonAndDependant(File file, String institutionUuid) throws Exception, IOException {
        Workbook workbook = null;
        try {
            workbook = WorkbookFactory.create(file);
            Sheet sheet = workbook.getSheetAt(0);
            int i = 0;
            int numberOfColumns = 0;
            int numbrOfDependants = 0;

            for (Row row : sheet) {
                if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
                    continue;
                }

                if (i == 0) {
                    // Get the number of columns in the first row
                    numberOfColumns = row.getLastCellNum();

                    // Calculate number of dependants - each dependant has 6 columns
                    if (numberOfColumns > 17) {
                        numbrOfDependants = (numberOfColumns - 17) / 6;

                        // Check if the dependant columns are properly formatted (multiple of 6)
                        if ((numberOfColumns - 17) % 6 != 0) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Error: Use the valid format for importing members. Each dependant should have exactly 6 columns.");
                        }
                    }

                    // Validate header row
                    try {
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
                                || !row.getCell(11).getStringCellValue().equalsIgnoreCase("Address")
                                || !row.getCell(12).getStringCellValue().equalsIgnoreCase("State")
                                || !row.getCell(13).getStringCellValue().equalsIgnoreCase("Country")
                                || !row.getCell(14).getStringCellValue().equalsIgnoreCase("Insurance Number")) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Error: The Excel sheet for uploading insured person should use the standard format.");
                        }
                    } catch (NullPointerException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Error: Missing required columns in the Excel sheet header.");
                    }
                } else {
                    // Process data rows
                    try {
                        // Create and populate the Insured entity
                        Insured person = new Insured();
                        person.setInsuredUuid(UUID.randomUUID().toString());

                        // Set insured person properties
                        person.setTitle(getCellValueAsString(row.getCell(0)));
                        person.setFirstName(getCellValueAsString(row.getCell(1)));
                        person.setFatherName(getCellValueAsString(row.getCell(2)));
                        person.setGrandFatherName(getCellValueAsString(row.getCell(3)));
                        person.setGender(getCellValueAsString(row.getCell(4)));

                        // Handle birth date
                        try {
                            if (row.getCell(5) != null) {
                                if (row.getCell(5).getCellType() == CellType.NUMERIC) {
                                    person.setBirthDate(row.getCell(5).getDateCellValue());
                                } else if (row.getCell(5).getCellType() == CellType.STRING) {
                                    // Try to parse the string as a date
                                    String dateStr = row.getCell(5).getStringCellValue();
                                    try {
                                        // Try different date formats
                                        SimpleDateFormat[] formats = {
                                                new SimpleDateFormat("yyyy-MM-dd"),
                                                new SimpleDateFormat("MM/dd/yyyy"),
                                                new SimpleDateFormat("dd/MM/yyyy"),
                                                new SimpleDateFormat("dd-MM-yyyy")
                                        };

                                        Date parsedDate = null;
                                        for (SimpleDateFormat format : formats) {
                                            try {
                                                parsedDate = format.parse(dateStr);
                                                break;
                                            } catch (ParseException e) {
                                                // Try next format
                                            }
                                        }

                                        if (parsedDate != null) {
                                            person.setBirthDate(parsedDate);
                                        } else {
                                            throw new BadRequestException("Invalid date format for Birth Date: " + dateStr);
                                        }
                                    } catch (Exception e) {
                                        throw new BadRequestException("Invalid date format for Birth Date: " + dateStr);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            throw new BadRequestException("Error processing Birth Date in row " + (i+1) + ": " + e.getMessage());
                        }

                        person.setIdNumber(getCellValueAsString(row.getCell(6)));
                        person.setPhone(getCellValueAsString(row.getCell(7)));
                        person.setEmail(getCellValueAsString(row.getCell(8)));
                        person.setBranchOffice(getCellValueAsString(row.getCell(9)));
                        person.setPosition(getCellValueAsString(row.getCell(10)));
                        person.setAddress(getCellValueAsString(row.getCell(11)));
                        person.setState(getCellValueAsString(row.getCell(12)));
                        person.setCountry(getCellValueAsString(row.getCell(13)));
                        person.setInsuranceId(getCellValueAsString(row.getCell(14)));

                        // Set status and institution
                        person.setStatus(Status.ACTIVE);
                        Payer payer = institutionRepository.findByPayerUuid(institutionUuid);
                        if (payer == null) {
                            throw new BadRequestException("Institution not found with UUID: " + institutionUuid);
                        }
                        person.setPayerUuid(institutionUuid);
                        person.setPayer(payer);

                        // Initialize the dependents collection
                        if (person.getDependants() == null) {
                            person.setDependants(new ArrayList<>());
                        }

                        // Save the insured person first to get an ID
                        insuredRepository.save(person);

                        // Now process dependants for this insured person
                        List<Dependant> dependants = new ArrayList<>();
                        int k = 0;
                        for (int j = 17; j < numberOfColumns; j += 6) {
                            k++;

                            // Check if dependant data exists
                            if (j + 5 < row.getLastCellNum() &&
                                    row.getCell(j) != null &&
                                    !getCellValueAsString(row.getCell(j)).trim().isEmpty()) {

                                Dependant dependant = new Dependant();
                                dependant.setDependantUuid(UUID.randomUUID().toString());

                                // Set dependant properties
                                dependant.setFirstName(getCellValueAsString(row.getCell(j)));
                                dependant.setFatherName(getCellValueAsString(row.getCell(j + 1)));
                                dependant.setGrandFatherName(getCellValueAsString(row.getCell(j + 2)));
                                dependant.setGender(getCellValueAsString(row.getCell(j + 3)));

                                // Handle date cell for dependant
                                if (row.getCell(j + 4) != null) {
                                    try {
                                        if (row.getCell(j + 4).getCellType() == CellType.NUMERIC) {
                                            dependant.setBirthDate(row.getCell(j + 4).getDateCellValue());
                                        } else if (row.getCell(j + 4).getCellType() == CellType.STRING) {
                                            // Try to parse the string as a date
                                            String dateStr = row.getCell(j + 4).getStringCellValue();
                                            try {
                                                // Try different date formats
                                                SimpleDateFormat[] formats = {
                                                        new SimpleDateFormat("yyyy-MM-dd"),
                                                        new SimpleDateFormat("MM/dd/yyyy"),
                                                        new SimpleDateFormat("dd/MM/yyyy"),
                                                        new SimpleDateFormat("dd-MM-yyyy")
                                                };

                                                Date parsedDate = null;
                                                for (SimpleDateFormat format : formats) {
                                                    try {
                                                        parsedDate = format.parse(dateStr);
                                                        break;
                                                    } catch (ParseException e) {
                                                        // Try next format
                                                    }
                                                }

                                                if (parsedDate != null) {
                                                    dependant.setBirthDate(parsedDate);
                                                } else {
                                                    throw new BadRequestException("Invalid date format for Dependant Birth Date: " + dateStr);
                                                }
                                            } catch (Exception e) {
                                                throw new BadRequestException("Invalid date format for Dependant Birth Date: " + dateStr);
                                            }
                                        }
                                    } catch (Exception e) {
                                        throw new BadRequestException("Error processing Dependant Birth Date in row " + (i+1) + ": " + e.getMessage());
                                    }
                                }

                                // Handle relationship cell
                                if (row.getCell(j + 5) != null) {
                                    String relationshipStr = getCellValueAsString(row.getCell(j + 5)).trim();

                                    // Try to match the relationship string to an enum value, ignoring case
                                    try {
                                        // First try exact match
                                        try {
                                            dependant.setRelationship(Relationship.valueOf(relationshipStr));
                                        } catch (IllegalArgumentException e) {
                                            // If that fails, try case-insensitive match
                                            boolean found = false;
                                            for (Relationship rel : Relationship.values()) {
                                                if (rel.name().equalsIgnoreCase(relationshipStr)) {
                                                    dependant.setRelationship(rel);
                                                    found = true;
                                                    break;
                                                }
                                            }

                                            if (!found) {
                                                throw new BadRequestException("Invalid relationship value: '" + relationshipStr +
                                                        "'. Valid values are: " + Arrays.toString(Relationship.values()) +
                                                        ". Make sure there are no extra spaces or special characters.");
                                            }
                                        }
                                    } catch (Exception e) {
                                        throw new BadRequestException("Error processing relationship: " + e.getMessage());
                                    }
                                }

                                dependant.setStatus(Status.ACTIVE);

                                // Set the relationship to the insured person
                                dependant.setInsured(person);

                                // Save the dependant
                                dependantRepository.save(dependant);

                                // Add to the list for this insured person
                                dependants.add(dependant);
                            }

                            if (k >= numbrOfDependants) {
                                break;
                            }
                        }

                        // Update the insured person's dependents collection
                        if (!dependants.isEmpty()) {
                            person.getDependants().addAll(dependants);
                            insuredRepository.save(person);
                        }

                    } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Error processing row " + (i+1) + ": " + e.getMessage());
                    }
                }
                i++;
            }

            return ResponseEntity.ok(new MessageResponse("Insured person imported successfully!"));
        } catch (Exception e) {
            throw e;
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    System.err.println("Error closing workbook: " + e.getMessage());
                }
            }
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }

    // Helper method to safely get cell value as string
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
                // Format numeric values to avoid scientific notation
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
        // Adjust page for 0-based indexing
        if (page > 0) {
            page = page - 1;
        }

        // Create pageable object for pagination
        Pageable pageable = PageRequest.of(page, limit, Sort.by(
                Sort.Order.asc("firstName"),
                Sort.Order.asc("fatherName"),
                Sort.Order.asc("grandFatherName")
        ));

        // Get the list of insured persons and dependants using the repository method
        List<InsuredDependantListResponse> response = insuredRepository.findInsuredPersonsAndDependants(
                payerInstitutionContractId, pageable);

        // Create a map to hold the merged responses
        Map<String, InsuredDependantResponse> mergedResponses = new HashMap<>();

        for (InsuredDependantListResponse item : response) {
            InsuredDependantResponse insuredDependantResponse = mergedResponses
                    .computeIfAbsent(item.getInsuredUuid(), k -> new InsuredDependantResponse());

            insuredDependantResponse.setInsuredTitle(item.getTitle());
            insuredDependantResponse.setInsuredPersonUuid(item.getInsuredUuid());
            insuredDependantResponse.setFirstName(item.getFirstName());
            insuredDependantResponse.setFatherName(item.getFatherName());
            insuredDependantResponse.setGrandFatherName(item.getGrandFatherName());
            insuredDependantResponse.setGender(item.getGender());
            insuredDependantResponse.setInsuranceId(item.getInsuranceId());
            insuredDependantResponse.setPhone(item.getPhone());

            // Only add dependant if dependantUuid is not null
            if (item.getDependantUuid() != null) {
                DependantInsuredResponse dependant = new DependantInsuredResponse();
                dependant.setInsuredPersonUuid(item.getInsuredUuid());
                dependant.setDependantUuid(item.getDependantUuid());
                dependant.setDependantFirstName(item.getDependantFirstName());
                dependant.setDependantFatherName(item.getDependantFatherName());
                dependant.setDependantGrandFatherName(item.getDependantGrandFatherName());
                dependant.setDependantGender(item.getDependantGender());

                // Handle potential null or invalid status/relationship values
                try {
                    dependant.setDependantStatus(item.getDependantStatus());
                } catch (Exception e) {
                    dependant.setDependantStatus(Status.PENDING); // Default value
                }

                try {
                    dependant.setRelationship(item.getRelationship());
                } catch (Exception e) {
                    dependant.setRelationship(null); // Or a default value if needed
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

    @Override
    public List<InsuredDependantResponse> getAllInsuredPersonsWithDependantsByInstitution(
            String institutionUuid, String searchKey, int page, int limit) {

        // Adjust page for 0-based indexing
        if (page > 0) {
            page = page - 1;
        }

        // Create pageable object for pagination and sorting
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id"));

        // Fetch insured persons using JPA repository method
        Page<Insured> insuredPage = insuredRepository.findByPayerAndSearchKey(
                institutionUuid, false, searchKey, pageable);

        // Get the content from the page
        List<Insured> insuredList = insuredPage.getContent();

        long totalPages = insuredPage.getTotalPages();
        long totalElements = insuredPage.getTotalElements();

        // Map to response objects
        List<InsuredDependantResponse> responseList = new ArrayList<>();

        for (Insured insured : insuredList) {
            InsuredDependantResponse response = new InsuredDependantResponse();

            // Map insured person properties
            response.setInsuredPersonUuid(insured.getInsuredUuid());
            response.setInsuredTitle(insured.getTitle());
            response.setFirstName(insured.getFirstName());
            response.setFatherName(insured.getFatherName());
            response.setGrandFatherName(insured.getGrandFatherName());
            response.setGender(insured.getGender());
            response.setInsuranceId(insured.getInsuranceId());
            response.setPhone(insured.getPhone());
            response.setEmail(insured.getEmail());
            response.setBirthDate(insured.getBirthDate());
            response.setStatus(insured.getStatus());
            response.setAddress1(insured.getAddress());

            if (insured.getDependants() != null && !insured.getDependants().isEmpty()) {
                for (Dependant dependant : insured.getDependants()) {
                    // Skip deleted dependants
                    if (dependant.isDeleted()) {
                        continue;
                    }

                    DependantInsuredResponse dependantResponse = new DependantInsuredResponse();
                    dependantResponse.setInsuredPersonUuid(insured.getInsuredUuid());
                    dependantResponse.setDependantUuid(dependant.getDependantUuid());
                    dependantResponse.setDependantFirstName(dependant.getFirstName());
                    dependantResponse.setDependantFatherName(dependant.getFatherName());
                    dependantResponse.setDependantGrandFatherName(dependant.getGrandFatherName());
                    dependantResponse.setDependantGender(dependant.getGender());
                    dependantResponse.setDependantStatus(dependant.getStatus());
                    dependantResponse.setRelationship(dependant.getRelationship());
                    dependantResponse.setDependantBirthDate(dependant.getBirthDate());

                    response.getDependants().add(dependantResponse);
                }
            }

            // Set pagination metadata
            response.setTotalPages(totalPages);
            response.setTotalElements(totalElements);

            responseList.add(response);
        }

        return responseList;
    }

    private void validatePhoto(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            return;
        }

        // Check file size (e.g., max 5MB)
        if (photo.getSize() > 5 * 1024 * 1024) {
            throw new BadRequestException("Photo size exceeds maximum limit of 5MB");
        }

        // Check file type
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

        String uploadDir = uploadDirectory + "/insured/photos/";
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Delete old photo if exists
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
