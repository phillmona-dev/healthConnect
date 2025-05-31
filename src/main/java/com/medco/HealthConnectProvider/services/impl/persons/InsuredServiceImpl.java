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
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.transaction.Transactional;


@Service
public class InsuredServiceImpl implements InsuredService {

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
    public ResponseEntity<?> createInsuredPerson(InsuredRequest insuredRequest) {

        if (insuredRepository.existsByEmailAndPayerPayerUuid(insuredRequest.getEmail(),
                insuredRequest.getPayerUuid())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Error: Email is already in use!");

        }

        if (insuredRepository.existsByPhoneAndPayerPayerUuid(insuredRequest.getPhone(),
                insuredRequest.getPayerUuid())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Error: Mobile Phone is already in use!");

        }

//		if (insuredRepository.existsByInsuranceIdAndInstitutionUuid(insuredRequest.getInsuranceId(),
//				insuredRequest.getInstitutionUuid())) {
//			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
//					"Error: Insurance Number is already in use!");
//
//		}

//		if (insuredRequest.getPayerInstitutionContractUuid() == null || !payerInstitutionContractRepository
//				.existsByPayerInstitutionContractUuid(insuredRequest.getPayerInstitutionContractUuid()))
//			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
//					"Error: Error: Insured Person should have payerInstitutionContractUuid!");

        Insured insured = new Insured();
        BeanUtils.copyProperties(insuredRequest, insured);
        if (insuredRequest.getStatus() != null)
            insured.setStatus(insuredRequest.getStatus());
        else
            insured.setStatus(Status.PENDING);
        Payer payer=institutionRepository.findByPayerUuid(insuredRequest.getPayerUuid());
        if (payer==null)throw new BadRequestException("institution not found");
        insured.setPayer(payer);

        insuredRepository.save(insured);
        return ResponseEntity.ok(new MessageResponse("Insured person registered successfully!"));
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
        response.setAddress1(insured.getAddress1());
        response.setAddress2(insured.getAddress2());

        // Add dependants if they exist
        if (insured.getDependents() != null && !insured.getDependents().isEmpty()) {
            for (Dependant dependant : insured.getDependents()) {
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
    public ResponseEntity<?> updateInsuredPersonWithDependants(String insuredUuid, InsuredWithDependantsRequest insuredRequest) {
        // Find the insured person
        Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
        if (insured == null)
            throw new ResourceNotFoundException("Insured Person", "insuredPersonUuid", insuredUuid);

        // Update insured person properties
        insured.setFirstName(insuredRequest.getFirstName());
        insured.setFatherName(insuredRequest.getFatherName());
        insured.setGrandFatherName(insuredRequest.getGrandFatherName());
        insured.setGender(insuredRequest.getGender());
        insured.setInsuranceId(insuredRequest.getInsuranceId());
        insured.setPhone(insuredRequest.getPhone());
        insured.setEmail(insuredRequest.getEmail());
        insured.setTitle(insuredRequest.getInsuredTitle());
        insured.setBirthDate(insuredRequest.getBirthDate());
        insured.setAddress1(insuredRequest.getAddress1());
        insured.setAddress2(insuredRequest.getAddress2());
        insured.setAddress3(insuredRequest.getAddress3());

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

        // Save the insured person
        insuredRepository.save(insured);

        // Process dependants if provided
        if (insuredRequest.getDependants() != null && !insuredRequest.getDependants().isEmpty()) {
            // Create a map of existing dependants by UUID for quick lookup
            Map<String, Dependant> existingDependants = new HashMap<>();
            if (insured.getDependents() != null) {
                for (Dependant dependant : insured.getDependents()) {
                    if (!dependant.isDeleted()) {
                        existingDependants.put(dependant.getDependantUuid(), dependant);
                    }
                }
            }

            // Process each dependant in the request
            for (DependantRequest dependantRequest : insuredRequest.getDependants()) {
                // Skip empty dependant entries
                if (isEmptyDependant(dependantRequest)) {
                    continue;
                }

                if (dependantRequest.getDependantUuid() != null && !dependantRequest.getDependantUuid().isEmpty()) {
                    // Update existing dependant
                    Dependant existingDependant = existingDependants.get(dependantRequest.getDependantUuid());
                    if (existingDependant != null) {
                        // Update properties
                        updateDependantProperties(existingDependant, dependantRequest);

                        // Save the updated dependant
                        dependantRepository.save(existingDependant);

                        // Remove from the map to track which ones were processed
                        existingDependants.remove(dependantRequest.getDependantUuid());
                    }
                } else if (isValidNewDependant(dependantRequest)) {
                    // Create new dependant only if all required fields are provided
                    Dependant newDependant = new Dependant();
                    newDependant.setDependantUuid(UUID.randomUUID().toString());
                    updateDependantProperties(newDependant, dependantRequest);

                    // Set the relationship to the insured person
                    newDependant.setInsured(insured);

                    // Save the new dependant
                    dependantRepository.save(newDependant);

                    // Add to the insured person's dependents collection
                    if (insured.getDependents() == null) {
                        insured.setDependents(new ArrayList<>());
                    }
                    insured.getDependents().add(newDependant);
                }
            }

            // Mark any remaining dependants as deleted if they weren't in the request
            for (Dependant remainingDependant : existingDependants.values()) {
                remainingDependant.setDeleted(true);
                dependantRepository.save(remainingDependant);
            }

            // Save the insured person again to update the dependents collection
            insuredRepository.save(insured);
        }

        return ResponseEntity.ok(new MessageResponse("Insured person and dependants updated successfully!"));
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

                person.setAddress1(row.getCell(11).getStringCellValue());
                person.setAddress2(row.getCell(12).getStringCellValue());
                person.setAddress3(row.getCell(13).getStringCellValue());
                person.setState(row.getCell(14).getStringCellValue());
                person.setCountry(row.getCell(15).getStringCellValue());
                person.setInsuranceId(row.getCell(16).getStringCellValue());
                person.setBeginDate(row.getCell(17).getDateCellValue());
                person.setEndDate(row.getCell(18).getDateCellValue());

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
        insured.setProfilePicture(newFileName);
        insuredRepository.save(insured);
        return ResponseEntity.ok(new MessageResponse("Profile Picture Update Successfully."));
    }

    @Override
    public List<InsuredListResponse> getInsuredPersonEligiblity(String insuredUuid) {
        return insuredRepository.findInsuredPersonEligibility(insuredUuid);
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
                                || !row.getCell(11).getStringCellValue().equalsIgnoreCase("Address1")
                                || !row.getCell(12).getStringCellValue().equalsIgnoreCase("Address2")
                                || !row.getCell(13).getStringCellValue().equalsIgnoreCase("Address3")
                                || !row.getCell(14).getStringCellValue().equalsIgnoreCase("State")
                                || !row.getCell(15).getStringCellValue().equalsIgnoreCase("Country")
                                || !row.getCell(16).getStringCellValue().equalsIgnoreCase("Insurance Number")) {
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
                        person.setAddress1(getCellValueAsString(row.getCell(11)));
                        person.setAddress2(getCellValueAsString(row.getCell(12)));
                        person.setAddress3(getCellValueAsString(row.getCell(13)));
                        person.setState(getCellValueAsString(row.getCell(14)));
                        person.setCountry(getCellValueAsString(row.getCell(15)));
                        person.setInsuranceId(getCellValueAsString(row.getCell(16)));

                        // Set status and institution
                        person.setStatus(Status.ACTIVE);
                        Payer payer = institutionRepository.findByPayerUuid(institutionUuid);
                        if (payer == null) {
                            throw new BadRequestException("Institution not found with UUID: " + institutionUuid);
                        }
                        person.setPayerUuid(institutionUuid);
                        person.setPayer(payer);

                        // Initialize the dependents collection
                        if (person.getDependents() == null) {
                            person.setDependents(new ArrayList<>());
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
                            person.getDependents().addAll(dependants);
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
            response.setDependants(convertToDependantResponse(insured.getDependents()));
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
            response.setAddress1(insured.getAddress1());
            response.setAddress2(insured.getAddress2());

            if (insured.getDependents() != null && !insured.getDependents().isEmpty()) {
                for (Dependant dependant : insured.getDependents()) {
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
}
