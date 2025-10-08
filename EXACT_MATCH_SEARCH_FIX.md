# Exact Match Search Fix - Priority Order

## Problem Description

### Issue
When searching for `identifier=557`, the API returns the WRONG person:

**Request:**
```
GET /api/v1/healthConnect/integration/pharmacy/check?identifier=557
```

**Wrong Response:**
```json
{
    "message": "One insured person found.",
    "insuredPersons": [{
        "firstName": "Shibiru",
        "grandFatherName": "0911685575",  // ❌ Contains "557"
        "phone": "0911685575",             // ❌ Contains "557"
        "idNumber": "Yohannes"             // ❌ NOT "557"!
    }]
}
```

**Expected Response:**
```json
{
    "message": "Multiple insured persons found.",
    "insuredPersons": [
        {
            "firstName": "Abeba",
            "idNumber": "557",  // ✅ Exact match!
            ...
        },
        {
            "firstName": "Another Person",
            "idNumber": "557",  // ✅ Exact match!
            ...
        }
    ]
}
```

### Root Cause

**The search was using PARTIAL MATCHES (LIKE) before EXACT MATCHES:**

**OLD Search Order (WRONG):**
1. Phone (exact match) → "557" != "0911685575" → No match
2. **Name (LIKE match)** → "557" in "0911685**557**5" → **MATCHES!** ❌
3. ID Number (exact match) → Never reached!

**Problem:**
- Searching for "557" matched `grandFatherName = "0911685575"` because it contains "557"
- The search returned immediately after finding this partial match
- Never reached the exact `idNumber = "557"` match

### Data Quality Issue

The response shows:
```json
{
    "grandFatherName": "0911685575",  // ❌ This is a phone number, not a name!
    "idNumber": "Yohannes"             // ❌ This is a name, not an ID!
}
```

**This indicates data entry errors** - fields are swapped or incorrectly entered.

## Solution

### 1. Changed Search Priority Order ✅

**NEW Search Order (CORRECT):**

**PRIORITY 1: Exact ID Matches (before partial name matches)**
1. ID Number (exact match) → `idNumber = "557"` ✅
2. Employee ID (exact match) → `employeeId = "557"` ✅
3. National ID (exact match) → `nationalId = "557"` ✅
4. Insurance ID (exact match) → `insuranceId = "557"` ✅
5. Phone (exact match) → `phone = "557"` ✅

**PRIORITY 2: Partial Name Matches (LIKE queries)**
6. Name combinations (LIKE match) → `firstName/fatherName/grandFatherName LIKE '%557%'`

**PRIORITY 3: Fallback**
7. Multiple fields search

### 2. Updated Repository Query ✅

**Added `isDeleted` filter to name search:**

```java
@Query("SELECT i FROM Insured i WHERE i.isDeleted = false AND (" +
        "(LOWER(i.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
        "LOWER(i.fatherName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
        "LOWER(i.grandFatherName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) OR " +
        "...)")
List<Insured> findByFullNameCombinations(@Param("searchTerm") String searchTerm);
```

### 3. Updated Service Logic ✅

**New search implementation:**

```java
@Override
public MultipleInsuredResponse searchInsuredPersons(String identifier) {
    log.info("Searching for insured person with identifier: {}", identifier);
    List<Insured> insuredList = new ArrayList<>();

    // PRIORITY 1: Exact ID matches first
    
    // 1. Search by ID number (exact match)
    List<Insured> insuredByIdNumber = insuredRepository.findByIdNumber(identifier);
    if (insuredByIdNumber != null && !insuredByIdNumber.isEmpty()) {
        insuredList.addAll(insuredByIdNumber);
        log.info("Found {} insured person(s) by idNumber '{}'", insuredList.size(), identifier);
        return new MultipleInsuredResponse(mapToInsuredSearchResponses(insuredList));
    }

    // 2. Search by employee ID (exact match)
    Insured insuredByEmployeeId = insuredRepository.findByEmployeeId(identifier);
    if (insuredByEmployeeId != null) {
        insuredList.add(insuredByEmployeeId);
        return new MultipleInsuredResponse(mapToInsuredSearchResponses(insuredList));
    }

    // 3. Search by national ID (exact match)
    Insured insuredByNationalId = insuredRepository.findByNationalId(identifier);
    if (insuredByNationalId != null) {
        insuredList.add(insuredByNationalId);
        return new MultipleInsuredResponse(mapToInsuredSearchResponses(insuredList));
    }

    // 4. Search by insurance ID (exact match)
    Insured insuredByInsuranceId = insuredRepository.findByInsuranceId(identifier);
    if (insuredByInsuranceId != null) {
        insuredList.add(insuredByInsuranceId);
        return new MultipleInsuredResponse(mapToInsuredSearchResponses(insuredList));
    }

    // 5. Search by phone (exact match)
    Collection<? extends Insured> insuredByPhone = insuredRepository.findByPhone(identifier);
    if (insuredByPhone != null && !insuredByPhone.isEmpty()) {
        insuredList.addAll(insuredByPhone);
        return new MultipleInsuredResponse(mapToInsuredSearchResponses(insuredList));
    }

    // PRIORITY 2: Partial name matches (LIKE queries)
    
    // 6. Search by full name combinations (partial match)
    List<Insured> insuredByName = insuredRepository.findByFullNameCombinations(identifier);
    if (insuredByName != null && !insuredByName.isEmpty()) {
        insuredList.addAll(insuredByName);
        return new MultipleInsuredResponse(mapToInsuredSearchResponses(insuredList));
    }

    // PRIORITY 3: Final fallback
    List<Insured> insuredByMultipleFields = insuredRepository.findByPhoneOrEmployeeIdOrNationalId(
            identifier, identifier, identifier);
    
    if (insuredByMultipleFields != null && !insuredByMultipleFields.isEmpty()) {
        insuredList.addAll(insuredByMultipleFields);
    }

    return new MultipleInsuredResponse(mapToInsuredSearchResponses(insuredList));
}
```

## Why This Works

### Example: Searching for "557"

**OLD Behavior (WRONG):**
```
Step 1: Phone exact match → "557" != "0911685575" → No match
Step 2: Name LIKE match → "557" in "0911685575" → MATCH! ❌
        Returns: Shibiru (wrong person)
```

**NEW Behavior (CORRECT):**
```
Step 1: ID Number exact match → idNumber = "557" → MATCH! ✅
        Returns: Abeba + other people with idNumber = "557"
```

### Example: Searching for "Abeba Afework"

**NEW Behavior:**
```
Step 1-5: No exact ID matches → Continue
Step 6: Name LIKE match → "Abeba Afework" in names → MATCH! ✅
        Returns: Abeba Afework
```

## Search Priority Comparison

| Search Type | OLD Priority | NEW Priority | Match Type |
|-------------|--------------|--------------|------------|
| Phone | 1 | 5 | Exact |
| Name | 2 | 6 | LIKE (partial) |
| ID Number | 3 | **1** ✅ | Exact |
| Employee ID | 4 | **2** ✅ | Exact |
| National ID | 5 | **3** ✅ | Exact |
| Insurance ID | 6 | **4** ✅ | Exact |
| Multiple Fields | 7 | 7 | Exact |

**Key Change:** Exact ID matches now have HIGHER priority than partial name matches.

## Benefits

### 1. Exact Matches Prioritized ✅
- Searching for "557" finds people with `idNumber = "557"`
- Not people with "557" in their phone number or name

### 2. Prevents False Positives ✅
- "557" won't match "0911685575" anymore
- More accurate search results

### 3. Better Performance ✅
- Exact matches are faster than LIKE queries
- Database can use indexes for exact matches

### 4. Handles Data Quality Issues ✅
- Even if grandFatherName contains phone numbers
- Exact ID matches take priority

## Testing

### Test Case 1: Search by ID Number
```bash
GET /api/v1/healthConnect/integration/pharmacy/check?identifier=557
```

**Expected:**
- Finds ALL people with `idNumber = "557"`
- Does NOT match phone "0911685575"

**Log Output:**
```
Found 2 insured person(s) by idNumber '557': 
  Abeba Afework (Payer: ..., idNumber: 557), 
  Another Person (Payer: ..., idNumber: 557)
```

### Test Case 2: Search by Name
```bash
GET /api/v1/healthConnect/integration/pharmacy/check?identifier=Abeba Afework
```

**Expected:**
- No exact ID matches
- Falls through to name search
- Finds people with names matching "Abeba Afework"

### Test Case 3: Search by Phone
```bash
GET /api/v1/healthConnect/integration/pharmacy/check?identifier=0911685575
```

**Expected:**
- No exact ID matches
- Finds people with `phone = "0911685575"`

## Data Quality Recommendations

### Issue Found
```json
{
    "grandFatherName": "0911685575",  // Phone number in name field
    "idNumber": "Yohannes"             // Name in ID field
}
```

### Recommended Actions

1. **Add Validation Rules:**
   - ID fields should only contain numbers/alphanumeric
   - Name fields should not contain phone numbers
   - Phone fields should match phone number format

2. **Clean Existing Data:**
   ```sql
   -- Find records with phone numbers in name fields
   SELECT id, first_name, father_name, grand_father_name, phone
   FROM insured
   WHERE grand_father_name ~ '^[0-9]+$'  -- Contains only numbers
      OR grand_father_name LIKE '09%';   -- Looks like Ethiopian phone
   
   -- Find records with names in ID fields
   SELECT id, id_number, first_name
   FROM insured
   WHERE id_number ~ '[a-zA-Z]'  -- Contains letters
     AND LENGTH(id_number) > 10;  -- Too long for typical ID
   ```

3. **Add Database Constraints:**
   ```sql
   -- Ensure phone numbers match format
   ALTER TABLE insured ADD CONSTRAINT check_phone_format 
   CHECK (phone ~ '^[0-9+\-\s()]+$' OR phone IS NULL);
   
   -- Ensure ID numbers don't contain only letters
   ALTER TABLE insured ADD CONSTRAINT check_id_number_format
   CHECK (id_number ~ '[0-9]' OR id_number IS NULL);
   ```

## Summary of Changes

| File | Change | Purpose |
|------|--------|---------|
| `InsuredRepository.java` | Added `isDeleted` filter to `findByFullNameCombinations` | Filter deleted records |
| `InsuredServiceImpl.java` | Reordered search priority | Exact matches before partial matches |
| `InsuredServiceImpl.java` | Removed duplicate searches | Clean up code |

## Next Steps

1. ✅ Restart your application
2. ✅ Test with `identifier=557`
3. ✅ Verify it returns people with `idNumber = "557"`
4. ✅ Check logs to see which field matched
5. ✅ Run data quality checks to find swapped fields
6. ✅ Add validation to prevent future data entry errors

**The search will now prioritize exact ID matches over partial name matches!** 🎉

