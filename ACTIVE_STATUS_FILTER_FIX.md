# Active Status Filter - Only Show ACTIVE Insured Persons

## Problem Description

### Issue
The pharmacy integration endpoint was returning insured persons with **INACTIVE** status:

**Request:**
```
GET /api/v1/healthConnect/integration/pharmacy/check?identifier=557
```

**Problem:**
- Returns both ACTIVE and INACTIVE insured persons
- INACTIVE persons should NOT be shown
- Only ACTIVE persons should be eligible for pharmacy services

### Business Context

**Status Values:**
- `ACTIVE` - Person is currently insured and eligible for services ✅
- `INACTIVE` - Person's insurance has expired or been terminated ❌
- Other statuses: `PENDING`, `SUSPENDED`, etc.

**Requirement:**
Only `ACTIVE` insured persons should be returned for pharmacy eligibility checks.

## Solution

### Updated All Repository Queries ✅

Added `status = 'ACTIVE'` filter to all search methods:

#### 1. Find by ID Number
```java
@Query("SELECT i FROM Insured i WHERE i.idNumber = :idNumber " +
       "AND i.isDeleted = false " +
       "AND i.status = 'ACTIVE' " +
       "ORDER BY i.id ASC")
List<Insured> findByIdNumber(@Param("idNumber") String idNumber);
```

#### 2. Find by Phone
```java
@Query("SELECT i FROM Insured i WHERE i.phone = :phone " +
       "AND i.isDeleted = false " +
       "AND i.status = 'ACTIVE' " +
       "ORDER BY i.id ASC")
Collection<? extends Insured> findByPhone(@Param("phone") String phone);
```

#### 3. Find by Employee ID
```java
@Query("SELECT i FROM Insured i WHERE i.employeeId = :employeeId " +
       "AND i.isDeleted = false " +
       "AND i.status = 'ACTIVE' " +
       "ORDER BY i.id ASC LIMIT 1")
Insured findByEmployeeId(@Param("employeeId") String employeeId);
```

#### 4. Find by National ID
```java
@Query("SELECT i FROM Insured i WHERE i.nationalId = :nationalId " +
       "AND i.isDeleted = false " +
       "AND i.status = 'ACTIVE' " +
       "ORDER BY i.id ASC LIMIT 1")
Insured findByNationalId(@Param("nationalId") String nationalId);
```

#### 5. Find by Insurance ID
```java
@Query("SELECT i FROM Insured i WHERE i.insuranceId = :insuranceId " +
       "AND i.isDeleted = false " +
       "AND i.status = 'ACTIVE' " +
       "ORDER BY i.id ASC LIMIT 1")
Insured findByInsuranceId(@Param("insuranceId") String insuranceId);
```

#### 6. Find by Full Name Combinations
```java
@Query("SELECT i FROM Insured i WHERE i.isDeleted = false " +
       "AND i.status = 'ACTIVE' " +
       "AND (" +
       "  (LOWER(i.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
       "   LOWER(i.fatherName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
       "   LOWER(i.grandFatherName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) OR " +
       "  (LOWER(CONCAT(i.firstName, ' ', i.fatherName)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
       "   ...)" +
       ")")
List<Insured> findByFullNameCombinations(@Param("searchTerm") String searchTerm);
```

#### 7. Find by Multiple Fields
```java
@Query("SELECT i FROM Insured i WHERE " +
       "(i.phone = :phone OR i.employeeId = :employeeId OR i.nationalId = :nationalId) " +
       "AND i.isDeleted = false " +
       "AND i.status = 'ACTIVE' " +
       "ORDER BY i.id ASC")
List<Insured> findByPhoneOrEmployeeIdOrNationalId(
    @Param("phone") String phone, 
    @Param("employeeId") String employeeId, 
    @Param("nationalId") String nationalId
);
```

## Filter Criteria

All search queries now filter by:

| Filter | Value | Purpose |
|--------|-------|---------|
| `isDeleted` | `false` | Exclude soft-deleted records |
| `status` | `'ACTIVE'` | Only show active insured persons |

## Before vs After

### Before (WRONG)
```sql
SELECT * FROM insured 
WHERE id_number = '557'
  AND is_deleted = false
-- ❌ Returns both ACTIVE and INACTIVE persons
```

**Result:**
```json
{
    "insuredPersons": [
        {
            "firstName": "John",
            "status": "ACTIVE"     // ✅ Should be shown
        },
        {
            "firstName": "Jane",
            "status": "INACTIVE"   // ❌ Should NOT be shown
        }
    ]
}
```

### After (CORRECT)
```sql
SELECT * FROM insured 
WHERE id_number = '557'
  AND is_deleted = false
  AND status = 'ACTIVE'
-- ✅ Returns only ACTIVE persons
```

**Result:**
```json
{
    "insuredPersons": [
        {
            "firstName": "John",
            "status": "ACTIVE"     // ✅ Shown
        }
        // Jane with INACTIVE status is filtered out
    ]
}
```

## Impact on Search Flow

### Search Priority (All with ACTIVE filter)

**PRIORITY 1: Exact ID Matches**
1. ✅ ID Number → `status = 'ACTIVE'`
2. ✅ Employee ID → `status = 'ACTIVE'`
3. ✅ National ID → `status = 'ACTIVE'`
4. ✅ Insurance ID → `status = 'ACTIVE'`
5. ✅ Phone → `status = 'ACTIVE'`

**PRIORITY 2: Partial Name Matches**
6. ✅ Name combinations → `status = 'ACTIVE'`

**PRIORITY 3: Fallback**
7. ✅ Multiple fields → `status = 'ACTIVE'`

## Status Management

### How Status Changes

**1. Manual Status Change:**
```java
// Update insured status
insured.setStatus(Status.INACTIVE);
insuredRepository.save(insured);
```

**2. Automatic Status Change (Scheduled):**
```java
@Scheduled(cron = "0 0 0 * * ?") // Daily at midnight
public void updateInactiveStatuses() {
    List<Insured> insuredList = insuredRepository
        .findByStatusNotAndInactiveDateBefore(Status.INACTIVE, new Date());
    
    for (Insured insured : insuredList) {
        insured.setStatus(Status.INACTIVE);
        insuredRepository.save(insured);
    }
}
```

**3. Policy End Date:**
- When `policyEndDate` is reached, status changes to `INACTIVE`
- Scheduled job runs daily to update statuses

### Status Lifecycle

```
NEW → ACTIVE → INACTIVE
       ↑         ↓
       └─────────┘
     (Can be reactivated)
```

## Testing

### Test Case 1: Search for ACTIVE Person
```bash
GET /api/v1/healthConnect/integration/pharmacy/check?identifier=557
```

**Database:**
```sql
-- Person 1: ACTIVE
INSERT INTO insured (id_number, first_name, status, is_deleted)
VALUES ('557', 'John', 'ACTIVE', false);

-- Person 2: INACTIVE
INSERT INTO insured (id_number, first_name, status, is_deleted)
VALUES ('557', 'Jane', 'INACTIVE', false);
```

**Expected Response:**
```json
{
    "message": "One insured person found.",
    "insuredPersons": [
        {
            "firstName": "John",
            "idNumber": "557",
            "status": "ACTIVE"  // ✅ Only ACTIVE person returned
        }
        // Jane (INACTIVE) is NOT returned
    ]
}
```

### Test Case 2: All Persons INACTIVE
```bash
GET /api/v1/healthConnect/integration/pharmacy/check?identifier=999
```

**Database:**
```sql
-- All persons with idNumber = '999' are INACTIVE
INSERT INTO insured (id_number, first_name, status, is_deleted)
VALUES ('999', 'Bob', 'INACTIVE', false);
```

**Expected Response:**
```json
{
    "message": "No insured persons found."
}
```

### Test Case 3: Multiple ACTIVE Persons
```bash
GET /api/v1/healthConnect/integration/pharmacy/check?identifier=123
```

**Database:**
```sql
-- Person 1: ACTIVE
INSERT INTO insured (id_number, first_name, status, payer_uuid)
VALUES ('123', 'Alice', 'ACTIVE', 'payer-1');

-- Person 2: ACTIVE (different payer)
INSERT INTO insured (id_number, first_name, status, payer_uuid)
VALUES ('123', 'Bob', 'ACTIVE', 'payer-2');

-- Person 3: INACTIVE (filtered out)
INSERT INTO insured (id_number, first_name, status, payer_uuid)
VALUES ('123', 'Charlie', 'INACTIVE', 'payer-3');
```

**Expected Response:**
```json
{
    "message": "Multiple insured persons found. Please select one.",
    "insuredPersons": [
        {
            "firstName": "Alice",
            "status": "ACTIVE"
        },
        {
            "firstName": "Bob",
            "status": "ACTIVE"
        }
        // Charlie (INACTIVE) is NOT returned
    ]
}
```

## Database Query Examples

### Check ACTIVE vs INACTIVE Counts
```sql
-- Count by status
SELECT 
    status,
    COUNT(*) as count
FROM insured
WHERE is_deleted = false
GROUP BY status;
```

### Find INACTIVE Persons That Would Be Filtered
```sql
-- Find persons that would be filtered out
SELECT 
    id,
    first_name,
    father_name,
    id_number,
    status,
    policy_end_date
FROM insured
WHERE is_deleted = false
  AND status = 'INACTIVE'
ORDER BY id DESC
LIMIT 20;
```

### Verify Filter Works
```sql
-- Test the ACTIVE filter
SELECT 
    id_number,
    COUNT(*) as total,
    SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as active_count,
    SUM(CASE WHEN status = 'INACTIVE' THEN 1 ELSE 0 END) as inactive_count
FROM insured
WHERE is_deleted = false
GROUP BY id_number
HAVING COUNT(*) > 1;
```

## Benefits

### 1. Data Integrity ✅
- Only eligible persons are shown
- INACTIVE persons cannot access services

### 2. Business Logic ✅
- Respects insurance policy lifecycle
- Prevents expired policies from being used

### 3. Security ✅
- Prevents unauthorized access
- Ensures only valid insurance is used

### 4. Performance ✅
- Indexed status column for fast filtering
- Reduces result set size

## Summary of Changes

| File | Change | Purpose |
|------|--------|---------|
| `InsuredRepository.java` | Added `status = 'ACTIVE'` to `findByIdNumber()` | Filter ACTIVE only |
| `InsuredRepository.java` | Added `status = 'ACTIVE'` to `findByPhone()` | Filter ACTIVE only |
| `InsuredRepository.java` | Added `status = 'ACTIVE'` to `findByEmployeeId()` | Filter ACTIVE only |
| `InsuredRepository.java` | Added `status = 'ACTIVE'` to `findByNationalId()` | Filter ACTIVE only |
| `InsuredRepository.java` | Added `status = 'ACTIVE'` to `findByInsuranceId()` | Filter ACTIVE only |
| `InsuredRepository.java` | Added `status = 'ACTIVE'` to `findByFullNameCombinations()` | Filter ACTIVE only |
| `InsuredRepository.java` | Added `status = 'ACTIVE'` to `findByPhoneOrEmployeeIdOrNationalId()` | Filter ACTIVE only |

## Next Steps

1. ✅ Restart your application
2. ✅ Test the pharmacy check endpoint
3. ✅ Verify only ACTIVE persons are returned
4. ✅ Check logs to confirm filtering works
5. ✅ Run database queries to verify data

**All search queries now filter by `status = 'ACTIVE'` - only active insured persons will be shown!** 🎉

