# Package Category Management System

## Overview

The Package Category Management System is a comprehensive feature that allows 
healthcare payers to configure service package categories (e.g., Inpatient, Outpatient, Dental etc...) 
with per-person limits. This system enables fine-grained control over service consumption 
and ensures patients stay within their allocated limits for different types of healthcare services.

## Business Requirements

### Core Functionality

- **Payers** can configure custom package categories (from logged-in user context)
- **Per-person limits** are set at the contract level for each category
- **Contract details** include category assignments for services
- **Services** can belong to multiple categories simultaneously
- **Patient service usage** automatically reduces from their category limits
- **Service requests** are rejected when limits are exceeded
- **Limits reset** based on contract periods

## System Architecture

### Database Schema

#### Core Tables
1. **`package_categories`** - Main category definitions
2. **`package_category_limits`** - Limits per contract and category
3. **`package_category_usage`** - Usage tracking per insured person
4. **`service_category_mappings`** - Service-to-category relationships

#### Key Relationships
- Categories belong to Payers (1:N)
- Limits link Categories to Contracts (N:N)
- Usage tracks consumption per Insured Person and Category Limit
- Services can map to multiple Categories (N:N)

### Entity Structure

```
PackageCategory
├── categoryUuid (Primary Key)
├── categoryName (e.g., "Inpatient")
├── categoryCode (e.g., "INP")
├── description
├── status (ACTIVE/INACTIVE)
├── payer (Foreign Key)
└── audit fields

PackageCategoryLimit
├── limitUuid (Primary Key)
├── packageCategory (Foreign Key)
├── contractHeader (Foreign Key)
├── limitType (AMOUNT/QUANTITY/VISITS)
├── limitValue (BigDecimal)
├── periodType (ANNUAL/MONTHLY/CONTRACT_PERIOD)
├── resetDate
└── audit fields

PackageCategoryUsage
├── usageUuid (Primary Key)
├── insuredPerson (Foreign Key)
├── categoryLimit (Foreign Key)
├── usedAmount/usedQuantity/usedVisits
├── serviceDate
├── claimUuid (Optional)
└── audit fields

ServiceCategoryMapping
├── mappingUuid (Primary Key)
├── contractDetail (Foreign Key)
├── packageCategory (Foreign Key)
├── consumesFromLimit (Boolean)
└── audit fields
```

## API Documentation

### Package Categories Management

#### Create Package Category
```http
POST /api/v1/healthConnect/package-categories
Content-Type: application/json
Authorization: Bearer {token}

{
  "categoryName": "Inpatient Services",
  "categoryCode": "INP",
  "description": "All inpatient medical services",
  "status": "ACTIVE"
}
```

#### Get Package Categories
```http
GET /api/v1/healthConnect/package-categories
?searchKey=dental&status=ACTIVE&page=0&size=10
```

#### Update Package Category
```http
PUT /api/v1/healthConnect/package-categories/{categoryUuid}
Content-Type: application/json

{
  "categoryName": "Updated Category Name",
  "categoryCode": "UPD",
  "description": "Updated description",
  "status": "ACTIVE"
}
```

### Category Limits Management

#### Set Category Limits for Contract
```http
POST /api/v1/healthConnect/package-category-limits/contract/{contractUuid}
Content-Type: application/json

[
  {
    "categoryUuid": "category-uuid-1",
    "limitType": "AMOUNT",
    "limitValue": 5000.00,
    "periodType": "ANNUAL",
    "resetDate": "2024-12-31",
    "description": "Annual inpatient limit"
  },
  {
    "categoryUuid": "category-uuid-2",
    "limitType": "VISITS",
    "limitValue": 12,
    "periodType": "ANNUAL",
    "resetDate": "2024-12-31",
    "description": "Annual outpatient visits"
  }
]
```

#### Get Category Limit Summary
```http
GET /api/v1/healthConnect/package-category-limits/summary/insured/{insuredUuid}/contract/{contractUuid}
```

**Response:**
```json
{
  "insuredPersonUuid": "insured-uuid",
  "insuredPersonName": "John Doe",
  "contractUuid": "contract-uuid",
  "contractName": "Standard Health Plan",
  "categoryLimits": [
    {
      "categoryUuid": "category-uuid-1",
      "categoryName": "Inpatient Services",
      "categoryCode": "INP",
      "limitType": "AMOUNT",
      "limitValue": 5000.00,
      "usedAmount": 1200.00,
      "remainingAmount": 3800.00,
      "periodType": "ANNUAL",
      "resetDate": "2024-12-31",
      "isExpired": false,
      "utilizationPercentage": 24.0
    }
  ]
}
```

### Usage Tracking

#### Record Service Consumption
```http
POST /api/v1/healthConnect/package-category-usage/record-consumption
?insuredUuid={uuid}&contractDetailUuid={uuid}&serviceAmount=150.00
&quantity=1&serviceDate=2024-01-15T10:30:00&claimUuid={uuid}
```

#### Get Usage by Claim
```http
GET /api/v1/healthConnect/package-category-usage/claim/{claimUuid}
```

#### Reverse Service Consumption
```http
DELETE /api/v1/healthConnect/package-category-usage/reverse/claim/{claimUuid}
```

### Service Category Mappings

#### Map Service to Categories
```http
POST /api/v1/healthConnect/service-category-mappings
Content-Type: application/json

{
  "contractDetailUuid": "service-uuid",
  "categoryUuids": ["category-1", "category-2"],
  "consumesFromLimit": true,
  "notes": "Emergency surgery service"
}
```

#### Get Categories by Service
```http
GET /api/v1/healthConnect/service-category-mappings/service/{contractDetailUuid}/categories
```

## Integration Points

### Eligibility Verification Enhancement

The eligibility service now includes category limit information in responses:

```json
{
  "serviceUuid": "service-uuid",
  "serviceName": "Emergency Surgery",
  "isCovered": true,
  "price": 2500.00,
  "hasAvailableLimit": true,
  "categoryLimits": [
    {
      "categoryName": "Inpatient Services",
      "limitValue": 5000.00,
      "remainingAmount": 3800.00,
      "utilizationPercentage": 24.0
    }
  ],
  "limitWarning": null
}
```

### Claims Processing Integration

Claims are automatically validated against category limits:

1. **Pre-validation**: Check if claim services can be consumed within limits
2. **Usage Recording**: Automatically record consumption when claims are approved
3. **Usage Reversal**: Reverse consumption when claims are cancelled/rejected

## Implementation Details

### Service Layer Components

#### PackageCategoryService
- CRUD operations for categories
- Validation for unique codes/names per provider
- Status management (activate/deactivate)

#### PackageCategoryLimitService
- Limit configuration per contract
- Automatic limit reset scheduling
- Remaining limit calculations
- Usage validation

#### PackageCategoryUsageService
- Service consumption recording
- Usage history tracking
- Claim-based usage reversal
- Validation before consumption

#### ClaimCategoryIntegrationService
- Claims validation against limits
- Automatic usage recording for approved claims
- Usage reversal for cancelled claims

### Key Features

#### Flexible Limit Types
- **AMOUNT**: Monetary limits (e.g., ETB5,000 annual)
- **QUANTITY**: Item/service quantity limits (e.g., 10 procedures)
- **VISITS**: Visit count limits (e.g., 12 visits per year)

#### Period Management
- **ANNUAL**: Yearly reset
- **MONTHLY**: Monthly reset
- **CONTRACT_PERIOD**: Reset based on contract duration
- **PER_CLAIM**: Per-claim basis (no accumulation)

#### Multi-Category Support
- Services can belong to multiple categories
- Consumption reduces from all applicable category limits
- Validation ensures all categories have sufficient limits

#### Automatic Reset
- Scheduled task runs daily to reset expired limits
- Creates new limit periods automatically
- Maintains historical usage data

## Error Handling

### Common Error Scenarios

#### Insufficient Limits
```json
{
  "error": "BadRequestException",
  "message": "Service consumption would exceed limit for category 'Inpatient'. Limit: 5000.00, Current usage: 4800.00, Requested: 500.00"
}
```

#### Invalid Category Code
```json
{
  "error": "BadRequestException", 
  "message": "Category code already exists for this provider: INP"
}
```

#### Resource Not Found
```json
{
  "error": "ResourceNotFoundException",
  "message": "Package category not found with UUID: invalid-uuid"
}
```

## Configuration

### Database Migration

Run the Liquibase migration to create the required tables:

```bash
mvn liquibase:update
```

### Application Properties

No additional configuration required. The system uses existing database and security configurations.

## Testing

### Unit Tests
- Service layer validation
- Repository query testing
- Business logic verification

### Integration Tests
- API endpoint testing
- Database transaction testing
- Cross-service integration

### Test Scenarios
1. Create categories and set limits
2. Map services to categories
3. Record service consumption
4. Validate limit enforcement
5. Test automatic limit reset
6. Verify claims integration

## Monitoring and Logging

### Key Metrics
- Category utilization rates
- Limit breach attempts
- Usage patterns by category
- System performance metrics

### Log Levels
- **INFO**: Normal operations, usage recording
- **WARN**: Approaching limits, validation failures
- **ERROR**: System errors, integration failures

## Security

### Authorization
- Provider-scoped category management
- Contract-based limit access
- Insured person data protection

### Validation
- Input sanitization
- Business rule enforcement
- Data integrity checks

## Performance Considerations

### Database Optimization
- Proper indexing on frequently queried columns
- Efficient pagination for large datasets
- Optimized usage calculation queries

### Caching Strategy
- Category definitions caching
- Limit calculations caching
- Usage summary caching

## Future Enhancements

### Potential Improvements
1. **Advanced Analytics**: Usage trend analysis and predictions
2. **Bulk Operations**: Import/export category configurations
3. **Notification System**: Alerts for approaching limits
4. **Reporting Dashboard**: Visual usage analytics
5. **Mobile API**: Simplified endpoints for mobile apps
6. **Audit Trail**: Detailed change tracking
7. **Approval Workflows**: Multi-level approval for limit changes

### Scalability Considerations
- Horizontal scaling for high-volume usage tracking
- Data archiving for historical usage records
- Performance optimization for real-time limit checking

## Implementation Files

### Entity Classes
```
src/main/java/com/medco/HealthConnectProvider/entity/packageCategory/
├── PackageCategory.java
├── PackageCategoryLimit.java
├── PackageCategoryUsage.java
└── ServiceCategoryMapping.java
```

### Repository Interfaces
```
src/main/java/com/medco/HealthConnectProvider/repository/packageCategory/
├── PackageCategoryRepository.java
├── PackageCategoryLimitRepository.java
├── PackageCategoryUsageRepository.java
└── ServiceCategoryMappingRepository.java
```

### Service Layer
```
src/main/java/com/medco/HealthConnectProvider/services/packageCategory/
├── PackageCategoryService.java
├── PackageCategoryLimitService.java
├── PackageCategoryUsageService.java
├── ServiceCategoryMappingService.java
└── ClaimCategoryIntegrationService.java

src/main/java/com/medco/HealthConnectProvider/services/impl/packageCategory/
├── PackageCategoryServiceImpl.java
├── PackageCategoryLimitServiceImpl.java
├── PackageCategoryUsageServiceImpl.java
├── ServiceCategoryMappingServiceImpl.java
└── ClaimCategoryIntegrationServiceImpl.java
```

### Controllers
```
src/main/java/com/medco/HealthConnectProvider/controller/packageCategory/
├── PackageCategoryController.java
├── PackageCategoryLimitController.java
├── PackageCategoryUsageController.java
└── ServiceCategoryMappingController.java
```

### DTOs
```
src/main/java/com/medco/HealthConnectProvider/ui/request/packageCategory/
├── PackageCategoryRequest.java
├── PackageCategoryLimitRequest.java
└── ServiceCategoryMappingRequest.java

src/main/java/com/medco/HealthConnectProvider/ui/response/packageCategory/
├── PackageCategoryResponse.java
├── PackageCategoryLimitResponse.java
├── PackageCategoryUsageResponse.java
└── CategoryLimitSummaryResponse.java
```

### Database Migration
```
src/main/resources/db/changelog/
└── 06-add-package-category-tables.xml
```

### Enums
```
src/main/java/com/medco/HealthConnectProvider/utils/enums/
├── LimitType.java
└── PeriodType.java
```

## Deployment Guide

### Prerequisites
- Java 17+
- PostgreSQL 12+
- Maven 3.8+
- Existing HealthConnect Provider application

### Deployment Steps

1. **Database Migration**
   ```bash
   mvn liquibase:update
   ```

2. **Build Application**
   ```bash
   mvn clean install
   ```

3. **Deploy Application**
   ```bash
   java -jar target/HealthConnectProvider-0.0.1-SNAPSHOT.jar
   ```

4. **Verify Deployment**
   - Check Swagger UI: `http://localhost:3012/swagger-ui.html`
   - Look for "Package Categories" section
   - Test basic CRUD operations

### Configuration Verification

Check that the following endpoints are available:
- `GET /api/v1/healthConnect/package-categories/provider/{providerUuid}/active`
- `POST /api/v1/healthConnect/package-category-limits/contract/{contractUuid}`
- `GET /api/v1/healthConnect/package-category-usage/claim/{claimUuid}`

## Usage Examples

### Complete Workflow Example

#### Step 1: Create Package Categories
```bash
# Create Inpatient Category (uses logged-in payer context)
curl -X POST "http://localhost:3012/api/v1/healthConnect/package-categories" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "categoryName": "Inpatient Services",
    "categoryCode": "INP",
    "description": "All inpatient medical services including surgeries and extended stays"
  }'

# Create Outpatient Category (uses logged-in payer context)
curl -X POST "http://localhost:3012/api/v1/healthConnect/package-categories" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "categoryName": "Outpatient Services",
    "categoryCode": "OUT",
    "description": "Outpatient consultations and minor procedures"
  }'
```

#### Step 2: Set Category Limits for Contract
```bash
curl -X POST "http://localhost:3012/api/v1/healthConnect/package-category-limits/contract/contract-uuid" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "categoryUuid": "inpatient-category-uuid",
      "limitType": "AMOUNT",
      "limitValue": 10000.00,
      "periodType": "ANNUAL",
      "resetDate": "2024-12-31",
      "description": "Annual inpatient coverage limit"
    },
    {
      "categoryUuid": "outpatient-category-uuid",
      "limitType": "VISITS",
      "limitValue": 24,
      "periodType": "ANNUAL",
      "resetDate": "2024-12-31",
      "description": "Annual outpatient visit limit"
    }
  ]'
```

#### Step 3: Map Services to Categories
```bash
curl -X POST "http://localhost:3012/api/v1/healthConnect/service-category-mappings" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "contractDetailUuid": "surgery-service-uuid",
    "categoryUuids": ["inpatient-category-uuid"],
    "consumesFromLimit": true,
    "notes": "Major surgical procedures"
  }'
```

#### Step 4: Check Eligibility (Enhanced Response)
```bash
curl -X GET "http://localhost:3012/api/v1/healthConnect/eligibility/check/provider-uuid" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "payerUuid": "payer-uuid",
    "insuranceId": "INS12345",
    "serviceUuid": "surgery-service-uuid"
  }'
```

**Enhanced Response with Category Limits:**
```json
{
  "eligible": true,
  "insuredPerson": {
    "insuredUuid": "insured-uuid",
    "firstName": "John",
    "lastName": "Doe"
  },
  "requestedService": {
    "serviceUuid": "surgery-service-uuid",
    "serviceName": "Cardiac Surgery",
    "isCovered": true,
    "price": 8500.00,
    "hasAvailableLimit": true,
    "categoryLimits": [
      {
        "categoryName": "Inpatient Services",
        "limitValue": 10000.00,
        "usedAmount": 0.00,
        "remainingAmount": 10000.00,
        "utilizationPercentage": 0.0
      }
    ],
    "limitWarning": null
  }
}
```

## Troubleshooting

### Common Issues

#### 1. Database Migration Fails
**Problem**: Liquibase migration fails with foreign key errors
**Solution**:
- Ensure base tables exist (payers, contract_headers, etc.)
- Check database user permissions
- Verify Liquibase configuration

#### 2. Category Code Uniqueness Violation
**Problem**: "Category code already exists" error
**Solution**:
- Category codes must be unique per payer
- Use validation endpoint to check before creation
- Consider using payer-specific prefixes

#### 3. Limit Validation Failures
**Problem**: Services rejected despite apparent available limits
**Solution**:
- Check if service is mapped to categories
- Verify limit period hasn't expired
- Ensure contract is active

#### 4. Usage Not Recording
**Problem**: Service consumption not reducing limits
**Solution**:
- Verify claims integration is enabled
- Check service-category mappings
- Ensure `consumesFromLimit` is true

### Debug Endpoints

#### Check Category Mappings
```bash
GET /api/v1/healthConnect/service-category-mappings/service/{contractDetailUuid}/categories
```

#### Validate Service Consumption
```bash
GET /api/v1/healthConnect/package-category-usage/validate-consumption
?insuredUuid={uuid}&contractDetailUuid={uuid}&serviceAmount=100.00
```

#### Get Usage History
```bash
GET /api/v1/healthConnect/package-category-usage/total/insured/{insuredUuid}
```

## Support and Maintenance

### Monitoring Queries

#### Check System Health
```sql
-- Category distribution by payer
SELECT p.payer_name, COUNT(pc.id) as category_count
FROM payers p
LEFT JOIN package_categories pc ON p.id = pc.payer_id
WHERE pc.is_deleted = false
GROUP BY p.payer_name;

-- Limit utilization summary
SELECT pc.category_name,
       AVG(pcl.limit_value) as avg_limit,
       AVG(usage_summary.total_used) as avg_usage
FROM package_categories pc
JOIN package_category_limits pcl ON pc.id = pcl.package_category_id
LEFT JOIN (
    SELECT category_limit_id, SUM(used_amount) as total_used
    FROM package_category_usage
    WHERE is_deleted = false
    GROUP BY category_limit_id
) usage_summary ON pcl.id = usage_summary.category_limit_id
GROUP BY pc.category_name;
```

#### Performance Monitoring
```sql
-- Slow usage queries
SELECT schemaname, tablename, attname, n_distinct, correlation
FROM pg_stats
WHERE tablename IN ('package_category_usage', 'package_category_limits')
ORDER BY n_distinct DESC;
```

### Backup Considerations

#### Critical Data
- Package category configurations
- Limit settings per contract
- Historical usage data
- Service-category mappings

#### Backup Strategy
```bash
# Backup category-related tables
pg_dump -h localhost -U postgres -t package_categories \
        -t package_category_limits -t package_category_usage \
        -t service_category_mappings healthConnect > category_backup.sql
```

---

## Conclusion

The Package Category Management System provides a robust, scalable solution for managing healthcare service limits with fine-grained control. The implementation follows best practices for enterprise applications and integrates seamlessly with the existing HealthConnect Provider system.

For additional support or feature requests, please refer to the main project documentation or contact the development team.
