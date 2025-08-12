# Bulk Service Mapping API Documentation

## Overview

The Bulk Service Mapping API allows you to efficiently map multiple services to package categories using either JSON requests or Excel file imports. This is essential for setting up category-based limits and tracking service consumption.

## API Endpoints

### 1. Process Bulk Service Mappings (JSON)

**Endpoint:** `POST /api/v1/healthConnect/bulk-service-mappings/process`

**Description:** Process multiple service-to-category mappings from JSON request

**Request Body:**
```json
{
  "contractUuid": "contract-uuid-here",
  "serviceMappings": [
    {
      "serviceCode": "SURG001",
      "serviceName": "Heart Surgery",
      "categoryCodes": ["INP", "SURG"],
      "consumesFromLimit": true,
      "notes": "Major surgical procedure"
    },
    {
      "serviceCode": "CONS001", 
      "serviceName": "General Consultation",
      "categoryCodes": ["OUT"],
      "consumesFromLimit": true,
      "notes": "Outpatient consultation"
    }
  ]
}
```

**Response:**
```json
{
  "contractUuid": "contract-uuid-here",
  "contractName": "Blue Cross Annual Contract",
  "totalServicesProcessed": 2,
  "successfulMappings": 2,
  "failedMappings": 0,
  "results": [
    {
      "serviceCode": "SURG001",
      "serviceName": "Heart Surgery", 
      "status": "SUCCESS",
      "message": "Successfully mapped to 2 categories",
      "mappedCategories": ["INP", "SURG"]
    }
  ],
  "errors": []
}
```

### 2. Import from Excel File

**Endpoint:** `POST /api/v1/healthConnect/bulk-service-mappings/import/{contractUuid}`

**Description:** Import service mappings from Excel file

**Parameters:**
- `contractUuid` (path): Contract UUID
- `file` (form-data): Excel file (.xlsx or .xls)

**Excel Format:**
| Service Code | Service Name | Category Codes (comma-separated) | Consumes From Limit | Notes |
|--------------|--------------|-----------------------------------|---------------------|-------|
| SURG001 | Heart Surgery | INP,SURG | true | Major surgical procedure |
| CONS001 | General Consultation | OUT | true | Outpatient consultation |
| DENT001 | Dental Cleaning | DEN | true | Routine dental care |

### 3. Download Excel Template

**Endpoint:** `GET /api/v1/healthConnect/bulk-service-mappings/template`

**Description:** Download Excel template for service mappings

**Response:** Excel file download

### 4. Validate Excel File

**Endpoint:** `POST /api/v1/healthConnect/bulk-service-mappings/validate-file`

**Description:** Validate Excel file format before processing

**Parameters:**
- `file` (form-data): Excel file to validate

**Response:**
```json
"File format is valid"
```

## Testing Workflow

### Step 1: Download Template
```bash
GET /api/v1/healthConnect/bulk-service-mappings/template
Authorization: Bearer {your-jwt-token}
```

### Step 2: Fill Template with Your Data
Open the downloaded Excel file and add your service mappings:

| Service Code | Service Name | Category Codes | Consumes From Limit | Notes |
|--------------|--------------|----------------|---------------------|-------|
| SURG001 | Heart Surgery | INP,SURG | true | Major surgical procedure |
| SURG002 | Appendectomy | INP | true | Minor surgical procedure |
| CONS001 | General Consultation | OUT | true | Outpatient consultation |
| CONS002 | Specialist Consultation | OUT | true | Specialist outpatient visit |
| DENT001 | Dental Cleaning | DEN | true | Routine dental cleaning |
| DENT002 | Dental Filling | DEN | true | Dental restoration |
| LAB001 | Blood Test | OUT,LAB | true | Laboratory test |
| XRAY001 | Chest X-Ray | OUT,RAD | true | Radiology service |

### Step 3: Validate File (Optional)
```bash
POST /api/v1/healthConnect/bulk-service-mappings/validate-file
Authorization: Bearer {your-jwt-token}
Content-Type: multipart/form-data

Form Data:
- file: [your-excel-file.xlsx]
```

### Step 4: Import Service Mappings
```bash
POST /api/v1/healthConnect/bulk-service-mappings/import/{contractUuid}
Authorization: Bearer {your-jwt-token}
Content-Type: multipart/form-data

Form Data:
- file: [your-excel-file.xlsx]
```

### Step 5: Verify Mappings
Check the response to see which mappings were successful:

```json
{
  "contractUuid": "contract-123",
  "contractName": "Blue Cross Annual Contract",
  "totalServicesProcessed": 8,
  "successfulMappings": 7,
  "failedMappings": 1,
  "results": [
    {
      "serviceCode": "SURG001",
      "status": "SUCCESS",
      "mappedCategories": ["INP", "SURG"]
    },
    {
      "serviceCode": "INVALID001", 
      "status": "FAILED",
      "message": "Service not found with code: INVALID001"
    }
  ]
}
```

## Alternative: JSON Request Method

If you prefer to use JSON instead of Excel:

```bash
POST /api/v1/healthConnect/bulk-service-mappings/process
Authorization: Bearer {your-jwt-token}
Content-Type: application/json

{
  "contractUuid": "your-contract-uuid",
  "serviceMappings": [
    {
      "serviceCode": "SURG001",
      "serviceName": "Heart Surgery",
      "categoryCodes": ["INP", "SURG"],
      "consumesFromLimit": true,
      "notes": "Major surgical procedure"
    },
    {
      "serviceCode": "CONS001",
      "serviceName": "General Consultation", 
      "categoryCodes": ["OUT"],
      "consumesFromLimit": true,
      "notes": "Outpatient consultation"
    }
  ]
}
```

## Error Handling

### Common Errors:
- **Service not found**: Service code doesn't exist in the system
- **Category not found**: Category code doesn't exist for the payer
- **Contract not found**: Invalid contract UUID
- **Mapping already exists**: Service already mapped to category
- **Invalid file format**: Excel file format is incorrect

### Response Status Codes:
- `200 OK`: Successful processing (check individual results)
- `400 Bad Request`: Invalid request or file format
- `401 Unauthorized`: Invalid or missing JWT token
- `404 Not Found`: Contract not found
- `500 Internal Server Error`: Server error during processing

## Best Practices

1. **Always download the template** to ensure correct format
2. **Validate files** before importing large datasets
3. **Use meaningful service codes** that match your system
4. **Check response results** for any failed mappings
5. **Test with small batches** before bulk imports
6. **Ensure categories exist** before mapping services to them

## Prerequisites

Before using this API, ensure you have:
1. Valid JWT token from payer user login
2. Existing contract with contract details
3. Created package categories (INP, OUT, DEN, etc.)
4. Services registered in the system with correct service codes
