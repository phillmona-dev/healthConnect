# Eligible Services Search API

## Overview

This API allows you to fetch all contract details (eligible services) for a selected category under a given contract with optional search and filtering capabilities. The main purpose is to retrieve services mapped to a specific category within a contract, with an optional search key for filtering.

## API Endpoint

**Endpoint:** `GET /api/v1/healthConnect/service-category-mappings/eligible-services`

**Description:** Fetch contract details (eligible services) for a selected category and contract with optional search

## Request Parameter

The API uses a single request parameter called `search` that contains all the search criteria:

**Required Fields in Search Parameter:**
- `contractUuid` (String) - Contract UUID to search within
- `categoryName` (String) - Category name to fetch services for

**Optional Fields in Search Parameter:**
- `searchKey` (String) - Search across service name, code, and description
- `categoryCode` (String) - Category code filter
- `categoryUuid` (String) - Category UUID filter
- `serviceCategory` (String) - Service category filter
- `serviceSubCategory` (String) - Service sub-category filter
- `minPrice` (BigDecimal) - Minimum price filter
- `maxPrice` (BigDecimal) - Maximum price filter
- `priceType` (String) - "SERVICE_PRICE" or "CONTRACT_PRICE"
- `status` (String) - "ACTIVE" or "INACTIVE"
- `consumesFromLimit` (Boolean) - Filter by limit consumption
- `isActive` (Boolean) - Filter by active status
- `providerName` (String) - Filter by provider name
- `sortBy` (String) - Sort field (default: "serviceName")
- `sortDirection` (String) - "ASC" or "DESC" (default: "ASC")
- `page` (Integer) - Page number (default: 0)
- `size` (Integer) - Page size (default: 20)

## URL Examples

### Basic Usage (Required Fields Only)
```
GET /api/v1/healthConnect/service-category-mappings/eligible-services?search.contractUuid=contract-123&search.categoryName=Inpatient Services
```

### With Search Key
```
GET /api/v1/healthConnect/service-category-mappings/eligible-services?search.contractUuid=contract-123&search.categoryName=Inpatient Services&search.searchKey=surgery
```

### With Advanced Filters
```
GET /api/v1/healthConnect/service-category-mappings/eligible-services?search.contractUuid=contract-123&search.categoryName=Inpatient Services&search.searchKey=heart&search.minPrice=5000&search.maxPrice=50000&search.sortBy=contractPrice&search.sortDirection=DESC&search.page=0&search.size=10
```

## Response Format

```json
{
  "content": [
    {
      "contractDetailUuid": "cd-uuid-1",
      "serviceUuid": "service-uuid-1",
      "serviceName": "Heart Surgery",
      "serviceCode": "SURG001",
      "serviceDescription": "Cardiac bypass surgery procedure",
      "serviceCategory": "Surgery",
      "serviceSubCategory": "Cardiac",
      "servicePrice": 45000.00,
      "contractPrice": 40000.00,
      "priceType": "NEGOTIATED_PRICE",
      "consumesFromLimit": true,
      "mappingNotes": "Major surgical procedure",
      "mappedAt": "2024-01-15T10:30:00Z",
      "mappedBy": "admin@hospital.com",
      "contractUuid": "contract-uuid-here",
      "contractName": "Blue Cross Annual Contract",
      "categoryUuid": "category-uuid-here",
      "categoryName": "Inpatient Services",
      "categoryCode": "INP",
      "providerUuid": "provider-uuid-here",
      "providerName": "City General Hospital",
      "status": "ACTIVE",
      "isActive": true,
      "additionalCategories": ["Surgical Procedures", "High-Cost Services"]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3,
  "last": false
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `contractDetailUuid` | String | Contract detail UUID |
| `serviceUuid` | String | Service UUID |
| `serviceName` | String | Service name |
| `serviceCode` | String | Service code |
| `serviceDescription` | String | Service description |
| `serviceCategory` | String | Service category |
| `serviceSubCategory` | String | Service sub-category |
| `servicePrice` | BigDecimal | Default service price |
| `contractPrice` | BigDecimal | Negotiated contract price |
| `priceType` | String | Price type (always "NEGOTIATED_PRICE") |
| `consumesFromLimit` | Boolean | Whether service consumes from category limits |
| `mappingNotes` | String | Notes from the mapping |
| `mappedAt` | Instant | When the mapping was created |
| `mappedBy` | String | Who created the mapping |
| `contractUuid` | String | Contract UUID |
| `contractName` | String | Contract name |
| `categoryUuid` | String | Category UUID |
| `categoryName` | String | Category name |
| `categoryCode` | String | Category code |
| `providerUuid` | String | Provider UUID |
| `providerName` | String | Provider name |
| `status` | String | Service status |
| `isActive` | Boolean | Whether mapping is active |
| `additionalCategories` | Array[String] | Other categories this service is mapped to |

## Testing Examples

### Example 1: Basic Category Fetch

```bash
GET /api/v1/healthConnect/service-category-mappings/eligible-services?search.contractUuid=contract-123-uuid&search.categoryName=Inpatient%20Services
Authorization: Bearer {your-jwt-token}
```

### Example 2: With Search Key

```bash
GET /api/v1/healthConnect/service-category-mappings/eligible-services?search.contractUuid=contract-123-uuid&search.categoryName=Inpatient%20Services&search.searchKey=surgery
Authorization: Bearer {your-jwt-token}
```

### Example 3: Advanced Filtering

```bash
GET /api/v1/healthConnect/service-category-mappings/eligible-services?search.contractUuid=contract-123-uuid&search.categoryName=Inpatient%20Services&search.searchKey=heart&search.serviceCategory=Surgery&search.minPrice=5000&search.maxPrice=50000&search.sortBy=contractPrice&search.sortDirection=DESC&search.page=0&search.size=10
Authorization: Bearer {your-jwt-token}
```

### Example 4: Filter by Status and Consumption

```bash
GET /api/v1/healthConnect/service-category-mappings/eligible-services?search.contractUuid=contract-123-uuid&search.categoryName=Outpatient%20Services&search.status=ACTIVE&search.consumesFromLimit=true&search.sortBy=serviceName
Authorization: Bearer {your-jwt-token}
```

## Real-World Use Cases

### Use Case 1: Review Inpatient Services
```bash
GET /api/v1/healthConnect/service-category-mappings/eligible-services?search.contractUuid=blue-cross-contract-uuid&search.categoryName=Inpatient%20Services&search.consumesFromLimit=true&search.sortBy=contractPrice&search.sortDirection=DESC&search.size=50
```

### Use Case 2: Find High-Cost Services in Category
```bash
GET /api/v1/healthConnect/service-category-mappings/eligible-services?search.contractUuid=contract-uuid&search.categoryName=Surgery&search.minPrice=10000&search.priceType=CONTRACT_PRICE&search.sortBy=contractPrice&search.sortDirection=DESC
```

### Use Case 3: Search for Specific Procedures
```bash
GET /api/v1/healthConnect/service-category-mappings/eligible-services?search.contractUuid=contract-uuid&search.categoryName=Outpatient%20Services&search.searchKey=consultation&search.serviceCategory=Consultation
```

### Use Case 4: Provider-Specific Services
```bash
GET /api/v1/healthConnect/service-category-mappings/eligible-services?search.contractUuid=contract-uuid&search.categoryName=Dental%20Services&search.providerName=City%20Dental%20Clinic
```

## Key Features

### 1. **Primary Purpose**
Fetch all services mapped to a specific category within a contract:
```bash
GET /api/v1/healthConnect/service-category-mappings/eligible-services?search.contractUuid=contract-uuid&search.categoryName=Inpatient%20Services
```

### 2. **Optional Search**
Add search functionality to filter within the category:
```bash
GET /api/v1/healthConnect/service-category-mappings/eligible-services?search.contractUuid=contract-uuid&search.categoryName=Inpatient%20Services&search.searchKey=surgery
```

### 3. **Price Range Filtering**
Filter services by price ranges:
```bash
GET /api/v1/healthConnect/service-category-mappings/eligible-services?search.contractUuid=contract-uuid&search.categoryName=Surgery&search.minPrice=1000&search.maxPrice=5000&search.priceType=CONTRACT_PRICE
```

### 4. **Provider-Specific Filtering**
Filter by provider within the category:
```bash
GET /api/v1/healthConnect/service-category-mappings/eligible-services?search.contractUuid=contract-uuid&search.categoryName=Dental%20Services&search.providerName=City%20General%20Hospital
```

## Single Request Parameter Benefits

### 1. **Clean API Design**
- Single `search` parameter contains all criteria
- No cluttered URL with multiple parameters
- Easy to understand and maintain

### 2. **Flexible Structure**
- All search fields are contained within the search object
- Easy to add new search criteria without changing the API signature
- Consistent with modern API design patterns

### 3. **Type Safety**
- Proper validation on the search object
- Required fields are clearly defined
- Optional fields have sensible defaults

## Error Handling

### Common Errors:
- **400 Bad Request**: Invalid search parameters
- **401 Unauthorized**: Invalid or missing JWT token
- **404 Not Found**: Contract not found
- **500 Internal Server Error**: Server error during search

### Response Status Codes:
- `200 OK`: Successful search
- `400 Bad Request`: Invalid request parameters
- `401 Unauthorized`: Authentication required
- `404 Not Found`: Contract not found
- `500 Internal Server Error`: Server error

## Performance Tips

1. **Use Specific Filters**: More specific filters improve performance
2. **Limit Page Size**: Use reasonable page sizes (20-50 items)
3. **Use Category UUID**: More efficient than category name
4. **Index-Friendly Sorts**: Use "serviceCode" or "serviceName" for better performance

## Prerequisites

Before using this API:
1. Valid JWT token from authenticated user
2. Existing contract with contract details
3. Services mapped to categories
4. Proper access permissions to the contract
