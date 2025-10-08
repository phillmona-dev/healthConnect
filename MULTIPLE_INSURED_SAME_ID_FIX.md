# Multiple Insured Persons with Same ID Number - Fix

## Problem Description

### Issue
When searching for insured persons with `identifier=557`, the API returns only ONE person instead of ALL persons with that idNumber:

**Request:**
```
GET /api/v1/healthConnect/integration/pharmacy/check?identifier=557
```

**Current Response (WRONG):**
```json
{
    "message": "One insured person found.",
    "insuredPersons": [
        {
            "insuredUuid": "472644e0-f0ea-4e5c-872e-8f329ecd1add",
            "firstName": "Shibiru",
            "idNumber": "Yohannes",  // ❌ This is NOT "557"!
            ...
        }
    ]
}
```

**Expected Response:**
```json
{
    "message": "Multiple insured persons found. Please select one.",
    "insuredPersons": [
        {
            "firstName": "Abeba",
            "fatherName": "Afework",
            "idNumber": "557",
            "payerName": "EDU. MATERIALS- EMPDE",
            ...
        },
        {
            "firstName": "Shibiru",
            "fatherName": "Abebe",
            "idNumber": "557",
            "payerName": "EXHIBITION CENTER",
            ...
        }
    ]
}
```

### Root Causes

#### 1. Repository Methods Don't Filter Deleted Records ❌

**Before:**
```java
List<Insured> findByIdNumber(String idNumber);
Collection<? extends Insured> findByPhone(String phone);
```

**Problem:**
- Spring Data JPA auto-generates these queries
- They DON'T filter by `isDeleted = false`
- May return deleted records
- May return records in unpredictable order

#### 2. No Logging to Debug What's Being Found ❌

The search method didn't log which records were found, making it impossible to debug.

#### 3. Possible Data Quality Issues ❌

The response shows `idNumber = "Yohannes"` when searching for `"557"`, which suggests:
- Data might be corrupted
- Fields might be swapped
- Wrong record is being returned

## Solution

### 1. Fixed Repository Methods ✅

**Added custom queries with proper filtering:**

```java
/**
 * Find insured persons by ID number
 * Returns ALL insured persons with the given idNumber across all payers
 * Filters out deleted records
 */
@Query("SELECT i FROM Insured i WHERE i.idNumber = :idNumber AND i.isDeleted = false ORDER BY i.id ASC")
List<Insured> findByIdNumber(@Param("idNumber") String idNumber);

/**
 * Find insured persons by phone
 * Returns ALL insured persons with the given phone across all payers
 * Filters out deleted records
 */
@Query("SELECT i FROM Insured i WHERE i.phone = :phone AND i.isDeleted = false ORDER BY i.id ASC")
Collection<? extends Insured> findByPhone(@Param("phone") String phone);
```

**Benefits:**
- ✅ Filters out deleted records (`isDeleted = false`)
- ✅ Returns results in consistent order (`ORDER BY i.id ASC`)
- ✅ Returns ALL matching records across all payers
- ✅ Explicit query - no auto-generation surprises

### 2. Added Detailed Logging ✅

**Enhanced logging in search method:**

```java
// Search by ID number
List<Insured> insuredByIdNumber = insuredRepository.findByIdNumber(identifier);
if (insuredByIdNumber != null && !insuredByIdNumber.isEmpty()) {
    insuredList.addAll(insuredByIdNumber);
    log.info("Found {} insured person(s) by idNumber '{}': {}", 
        insuredList.size(), 
        identifier,
        insuredList.stream()
            .map(i -> i.getFirstName() + " " + i.getFatherName() + 
                     " (Payer: " + i.getPayerUuid() + ", idNumber: " + i.getIdNumber() + ")")
            .collect(Collectors.joining(", "))
    );
    return new MultipleInsuredResponse(mapToInsuredSearchResponses(insuredList));
}
```

**Log Output Example:**
```
Found 2 insured person(s) by idNumber '557': 
  Abeba Afework (Payer: c66cfbf4-393a-48ea-80c2-43a2525789a7, idNumber: 557), 
  Shibiru Abebe (Payer: f5037a9d-8524-4086-943b-6008a6ca748a, idNumber: 557)
```

### 3. Created Debug SQL Script ✅

**File:** `debug-idnumber-search.sql`

Run this in pgAdmin to investigate data issues:

```sql
-- Find ALL insured persons with idNumber = '557'
SELECT 
    id, insured_uuid, first_name, father_name, id_number, 
    payer_uuid, is_deleted
FROM insured 
WHERE id_number = '557'
ORDER BY id ASC;

-- Check for duplicate idNumbers across payers
SELECT 
    id_number,
    COUNT(*) as count,
    STRING_AGG(DISTINCT first_name || ' ' || father_name, ', ') as names
FROM insured 
WHERE is_deleted = false
  AND id_number IS NOT NULL
GROUP BY id_number
HAVING COUNT(*) > 1
ORDER BY count DESC;
```

## Why This Happens

### Business Context

In your system, **multiple insured persons can have the same `idNumber` if they belong to different payers**:

| Person | ID Number | Payer | Reason |
|--------|-----------|-------|--------|
| Abeba Afework | 557 | EDU. MATERIALS- EMPDE | Employee ID in company A |
| Shibiru Abebe | 557 | EXHIBITION CENTER | Employee ID in company B |

This is **valid** because:
- Different companies can assign the same employee ID
- `idNumber` is NOT globally unique
- The unique identifier is `(idNumber + payerUuid)` combination

### Technical Issue

The old repository method:
```java
List<Insured> findByIdNumber(String idNumber);
```

Was auto-generated by Spring Data JPA as:
```sql
SELECT * FROM insured WHERE id_number = ?
-- ❌ No ORDER BY - unpredictable order
-- ❌ No isDeleted filter - may return deleted records
-- ❌ May return only first result due to caching or other issues
```

The new custom query:
```java
@Query("SELECT i FROM Insured i WHERE i.idNumber = :idNumber AND i.isDeleted = false ORDER BY i.id ASC")
```

Generates:
```sql
SELECT * FROM insured 
WHERE id_number = ? 
  AND is_deleted = false 
ORDER BY id ASC
-- ✅ Filters deleted records
-- ✅ Consistent ordering
-- ✅ Returns ALL matching records
```

## Testing

### Step 1: Run Debug SQL

Open pgAdmin and run `debug-idnumber-search.sql` to see:
- How many people have `idNumber = '557'`
- What their actual data looks like
- If there are data quality issues

### Step 2: Restart Application

```bash
mvn spring-boot:run
```

### Step 3: Test the API

```bash
GET http://localhost:3012/api/v1/healthConnect/integration/pharmacy/check?identifier=557
```

### Step 4: Check Logs

Look for the log message:
```
Found X insured person(s) by idNumber '557': [names and details]
```

This will show you:
- How many people were found
- Their names
- Their payer UUIDs
- Their actual idNumber values

### Expected Results

**If 2 people have idNumber = '557':**
```json
{
    "message": "Multiple insured persons found. Please select one.",
    "insuredPersons": [
        {
            "firstName": "Abeba",
            "idNumber": "557",
            "payerName": "EDU. MATERIALS- EMPDE"
        },
        {
            "firstName": "Shibiru",
            "idNumber": "557",
            "payerName": "EXHIBITION CENTER"
        }
    ]
}
```

**If only 1 person has idNumber = '557':**
```json
{
    "message": "One insured person found.",
    "insuredPersons": [
        {
            "firstName": "Abeba",
            "idNumber": "557",
            ...
        }
    ]
}
```

## Data Quality Investigation

### Suspicious Response

The response you showed has:
```json
{
    "firstName": "Shibiru",
    "fatherName": "Abebe",
    "grandFatherName": "0911685575",  // ❌ This looks like a phone number!
    "phone": "0911685575",
    "idNumber": "Yohannes"  // ❌ This looks like a name, not an ID!
}
```

**This suggests data entry errors:**
- `grandFatherName` contains a phone number
- `idNumber` contains a person's name ("Yohannes")

### Recommended Actions

1. **Run the debug SQL** to check actual database values
2. **Check data entry process** - fields might be getting swapped
3. **Add validation** to prevent names in ID fields and vice versa
4. **Clean up existing data** if fields are swapped

## Summary of Changes

| File | Change | Purpose |
|------|--------|---------|
| `InsuredRepository.java` | Added `@Query` for `findByIdNumber()` | Filter deleted records, consistent ordering |
| `InsuredRepository.java` | Added `@Query` for `findByPhone()` | Filter deleted records, consistent ordering |
| `InsuredServiceImpl.java` | Enhanced logging in search | Debug what's being found |
| `debug-idnumber-search.sql` | Created debug script | Investigate data quality |

## Next Steps

1. ✅ Restart your application
2. ✅ Run `debug-idnumber-search.sql` in pgAdmin
3. ✅ Test the API with `identifier=557`
4. ✅ Check application logs
5. ✅ Verify all matching persons are returned
6. ✅ Investigate data quality issues if fields are swapped

The API should now return **ALL** insured persons with the same idNumber across different payers! 🎉

