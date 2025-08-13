# Bulk Service Category Assignment API

## Overview

This API allows you to assign multiple contract details (eligible services) to a package category in a single request. The contract UUID is automatically extracted from the contract details' relationships, ensuring all services belong to the same contract.

## API Endpoint

**Endpoint:** `POST /api/v1/healthConnect/service-category-mappings/bulk-assign`

**Description:** Assign multiple contract details (eligible services) to a package category

## Request Format

```json
{
  "contractDetailUuids": [
    "contract-detail-uuid-1",
    "contract-detail-uuid-2", 
    "contract-detail-uuid-3"
  ],
  "categoryUuid": "category-uuid-here",
  "consumesFromLimit": true,
  "notes": "Bulk assignment of surgical procedures to inpatient category",
  "replaceExisting": false
}
```

### Request Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `contractDetailUuids` | Array[String] | Yes | List of contract detail UUIDs to assign |
| `categoryUuid` | String | Yes | Package category UUID to assign services to |
| `consumesFromLimit` | Boolean | Yes | Whether these services consume from category limits |
| `notes` | String | No | Optional notes for the assignment |
| `replaceExisting` | Boolean | No | If true, replaces existing mappings (default: false) |

## Response Format

```json
{
  "categoryUuid": "category-uuid-here",
  "categoryName": "Inpatient Services",
  "categoryCode": "INP",
  "contractUuid": "contract-uuid-here",
  "contractName": "Blue Cross Annual Contract",
  "totalServicesProcessed": 5,
  "successfulAssignments": 4,
  "failedAssignments": 1,
  "skippedAssignments": 0,
  "results": [
    {
      "contractDetailUuid": "contract-detail-uuid-1",
      "serviceName": "Heart Surgery",
      "serviceCode": "SURG001",
      "status": "SUCCESS",
      "message": "Successfully assigned to category"
    },
    {
      "contractDetailUuid": "contract-detail-uuid-2",
      "serviceName": "Appendectomy",
      "serviceCode": "SURG002", 
      "status": "SUCCESS",
      "message": "Successfully assigned to category"
    },
    {
      "contractDetailUuid": "contract-detail-uuid-3",
      "serviceName": "Invalid Service",
      "serviceCode": "INVALID001",
      "status": "FAILED",
      "message": "Contract detail not found"
    }
  ],
  "errors": []
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `categoryUuid` | String | Category UUID that services were assigned to |
| `categoryName` | String | Category name |
| `categoryCode` | String | Category code |
| `contractUuid` | String | Contract UUID (extracted from contract details) |
| `contractName` | String | Contract name |
| `totalServicesProcessed` | Integer | Total number of services processed |
| `successfulAssignments` | Integer | Number of successful assignments |
| `failedAssignments` | Integer | Number of failed assignments |
| `skippedAssignments` | Integer | Number of skipped assignments (already exist) |
| `results` | Array | Detailed results for each service |
| `errors` | Array | General errors (if any) |

### Result Status Values

| Status | Description |
|--------|-------------|
| `SUCCESS` | Service successfully assigned to category |
| `FAILED` | Assignment failed (see message for details) |
| `SKIPPED` | Assignment skipped (mapping already exists) |
| `REPLACED` | Existing mapping was replaced with new one |

## Testing Workflow

### Step 1: Get Contract Details

First, you need to get the contract detail UUIDs for the services you want to assign:

```bash
GET /api/v1/healthConnect/contracts/{contractUuid}/details
Authorization: Bearer {your-jwt-token}
```

This will return contract details with their UUIDs and associated services.

### Step 2: Assign Services to Category

```bash
POST /api/v1/healthConnect/service-category-mappings/bulk-assign
Authorization: Bearer {your-jwt-token}
Content-Type: application/json

{
  "contractDetailUuids": [
    "cd-001-uuid",
    "cd-002-uuid",
    "cd-003-uuid",
    "cd-004-uuid",
    "cd-005-uuid"
  ],
  "categoryUuid": "inpatient-category-uuid",
  "consumesFromLimit": true,
  "notes": "Assigning all surgical procedures to inpatient category",
  "replaceExisting": false
}
```

### Step 3: Verify Assignments

Check the response to see which assignments were successful:

```json
{
  "categoryUuid": "inpatient-category-uuid",
  "categoryName": "Inpatient Services",
  "categoryCode": "INP",
  "contractUuid": "contract-123-uuid",
  "contractName": "Blue Cross Annual Contract 2024",
  "totalServicesProcessed": 5,
  "successfulAssignments": 4,
  "failedAssignments": 0,
  "skippedAssignments": 1,
  "results": [
    {
      "contractDetailUuid": "cd-001-uuid",
      "serviceName": "Heart Surgery",
      "serviceCode": "SURG001",
      "status": "SUCCESS",
      "message": "Successfully assigned to category"
    },
    {
      "contractDetailUuid": "cd-002-uuid", 
      "serviceName": "Brain Surgery",
      "serviceCode": "SURG002",
      "status": "SKIPPED",
      "message": "Mapping already exists"
    }
  ]
}
```

## Real-World Example

### Scenario: Assigning Surgical Procedures to Inpatient Category

```bash
POST /api/v1/healthConnect/service-category-mappings/bulk-assign
Authorization: Bearer {your-jwt-token}
Content-Type: application/json

{
  "contractDetailUuids": [
    "cd-heart-surgery-uuid",
    "cd-brain-surgery-uuid", 
    "cd-appendectomy-uuid",
    "cd-gallbladder-surgery-uuid",
    "cd-knee-replacement-uuid"
  ],
  "categoryUuid": "inpatient-category-uuid",
  "consumesFromLimit": true,
  "notes": "All major surgical procedures require inpatient admission",
  "replaceExisting": false
}
```

**Expected Response:**
```json
{
  "categoryUuid": "inpatient-category-uuid",
  "categoryName": "Inpatient Services", 
  "categoryCode": "INP",
  "contractUuid": "blue-cross-contract-uuid",
  "contractName": "Blue Cross Insurance Annual Contract 2024",
  "totalServicesProcessed": 5,
  "successfulAssignments": 5,
  "failedAssignments": 0,
  "skippedAssignments": 0,
  "results": [
    {
      "contractDetailUuid": "cd-heart-surgery-uuid",
      "serviceName": "Cardiac Bypass Surgery",
      "serviceCode": "SURG001",
      "status": "SUCCESS",
      "message": "Successfully assigned to category"
    }
    // ... more results
  ]
}
```

## Error Handling

### Common Errors

1. **Contract Detail Not Found**
   - Status: `FAILED`
   - Message: "Contract detail not found"

2. **Category Not Found**
   - HTTP 404: "PackageCategory not found with UUID: {categoryUuid}"

3. **Payer Mismatch**
   - Status: `FAILED`
   - Message: "Category does not belong to the same payer as the contract"

4. **Mapping Already Exists**
   - Status: `SKIPPED`
   - Message: "Mapping already exists"

### Response Status Codes

- `200 OK`: Request processed (check individual results)
- `400 Bad Request`: Invalid request format
- `401 Unauthorized`: Invalid or missing JWT token
- `404 Not Found`: Category not found
- `500 Internal Server Error`: Server error

## Best Practices

1. **Batch Size**: Process services in batches of 50-100 for optimal performance
2. **Error Handling**: Always check individual results for failed assignments
3. **Validation**: Ensure all contract details belong to the same contract
4. **Replace Flag**: Use `replaceExisting: true` carefully as it removes existing mappings
5. **Notes**: Add meaningful notes for audit trail

## Prerequisites

Before using this API:
1. Valid JWT token from payer user login
2. Existing package category
3. Contract with contract details
4. Contract details must belong to the same payer as the category
