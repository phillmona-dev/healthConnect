# Services Unique Constraints Per Provider Fix

## 🐛 **The Problem**

When importing services for a provider, the system was throwing a duplicate key error:

```
ERROR: duplicate key value violates unique constraint "services_service_code_key"
Detail: Key (service_code)=(SRV001) already exists.
```

### **Root Cause:**

The `services` table had **global unique constraints** on:
- `service_code` - Must be unique across ALL providers
- `generated_service_id` - Must be unique across ALL providers

**Why This Was Wrong:**

- ❌ Provider A cannot use service code "SRV001" if Provider B already uses it
- ❌ Each provider should be able to define their own service codes
- ❌ Services are provider-specific, not global
- ❌ Importing the same service list for multiple providers fails

**Example of the Problem:**

```
Provider A (Hospital):
- Service Code: SRV001 = "General Consultation"

Provider B (Clinic):
- Service Code: SRV001 = "General Consultation" ❌ FAILS!
```

---

## ✅ **The Solution**

Changed unique constraints from **global** to **per provider** (composite unique constraints).

Now the same service code can exist for different providers, but not for the same provider.

**After Fix:**

```
Provider A (Hospital):
- Service Code: SRV001 = "General Consultation" ✅

Provider B (Clinic):
- Service Code: SRV001 = "General Consultation" ✅ (Different provider, allowed!)

Provider A (Hospital):
- Service Code: SRV001 = "X-Ray" ❌ (Same provider, duplicate!)
```

---

## 🔧 **What Changed**

### **1. Created Liquibase Changelog**

**File:** `src/main/resources/db/changelog/02-fix-services-unique-constraints-per-provider.xml`

**Changes:**

1. **Dropped global unique constraints:**
   - `services_service_code_key` (global)
   - `services_generated_service_id_key` (global)

2. **Added composite unique constraints:**
   - `uk_services_code_provider` on `(service_code, provider_id)`
   - `uk_services_generated_id_provider` on `(generated_service_id, provider_id)`

3. **Updated indexes for performance:**
   - Dropped: `idx_services_service_code`
   - Added: `idx_services_code_provider` on `(service_code, provider_id)`
   - Added: `idx_services_generated_id_provider` on `(generated_service_id, provider_id)`

### **2. Updated Master Changelog**

**File:** `src/main/resources/db/changelog/db.changelog-master.xml`

Added the new changeset:
```xml
<include file="db/changelog/02-fix-services-unique-constraints-per-provider.xml" relativeToChangelogFile="false"/>
```

### **3. Updated JPA Entity**

**File:** `src/main/java/com/medco/HealthConnectProvider/entity/services/Servicelist.java`

**Before:**
```java
@Entity
@Table(name = "services")
public class Servicelist extends Audit {
    
    @Column(unique = true)  // ❌ Global unique
    private String generatedServiceId;
    
    @Column(unique = true)  // ❌ Global unique
    private String serviceCode;
}
```

**After:**
```java
@Entity
@Table(name = "services", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_services_code_provider", 
                         columnNames = {"service_code", "provider_id"}),
        @UniqueConstraint(name = "uk_services_generated_id_provider", 
                         columnNames = {"generated_service_id", "provider_id"})
    },
    indexes = {
        @Index(name = "idx_services_code_provider", 
               columnList = "service_code, provider_id"),
        @Index(name = "idx_services_generated_id_provider", 
               columnList = "generated_service_id, provider_id")
    }
)
public class Servicelist extends Audit {
    
    @Column  // ✅ Unique per provider (via table constraint)
    private String generatedServiceId;
    
    @Column  // ✅ Unique per provider (via table constraint)
    private String serviceCode;
}
```

---

## 📊 **Database Schema Changes**

### **Before:**

```sql
CREATE TABLE services (
    id BIGSERIAL PRIMARY KEY,
    service_uuid VARCHAR(255) NOT NULL UNIQUE,
    generated_service_id VARCHAR(255) UNIQUE,  -- ❌ Global unique
    service_code VARCHAR(255) UNIQUE,          -- ❌ Global unique
    service_name VARCHAR(255) NOT NULL,
    provider_id BIGINT,
    ...
);

-- Global unique constraints
ALTER TABLE services ADD CONSTRAINT services_service_code_key 
    UNIQUE (service_code);
    
ALTER TABLE services ADD CONSTRAINT services_generated_service_id_key 
    UNIQUE (generated_service_id);
```

### **After:**

```sql
CREATE TABLE services (
    id BIGSERIAL PRIMARY KEY,
    service_uuid VARCHAR(255) NOT NULL UNIQUE,
    generated_service_id VARCHAR(255),  -- ✅ Unique per provider
    service_code VARCHAR(255),          -- ✅ Unique per provider
    service_name VARCHAR(255) NOT NULL,
    provider_id BIGINT,
    ...
);

-- Composite unique constraints (per provider)
ALTER TABLE services ADD CONSTRAINT uk_services_code_provider 
    UNIQUE (service_code, provider_id);
    
ALTER TABLE services ADD CONSTRAINT uk_services_generated_id_provider 
    UNIQUE (generated_service_id, provider_id);

-- Composite indexes for performance
CREATE INDEX idx_services_code_provider 
    ON services (service_code, provider_id);
    
CREATE INDEX idx_services_generated_id_provider 
    ON services (generated_service_id, provider_id);
```

---

## 🚀 **How to Apply**

### **Option 1: Automatic (Liquibase - Recommended)**

Liquibase will automatically apply the changes when you restart the application:

1. **Restart your application:**
   ```bash
   mvn spring-boot:run
   ```

2. **Check the logs:**
   ```
   INFO - Liquibase: Running Changeset: db/changelog/02-fix-services-unique-constraints-per-provider.xml::fix-services-unique-constraints-per-provider::system
   INFO - Liquibase: Unique constraint dropped
   INFO - Liquibase: Composite unique constraint added
   ```

3. **Verify in database:**
   ```sql
   -- Check constraints
   SELECT conname, contype, pg_get_constraintdef(oid) 
   FROM pg_constraint 
   WHERE conrelid = 'services'::regclass;
   
   -- Should show:
   -- uk_services_code_provider | u | UNIQUE (service_code, provider_id)
   -- uk_services_generated_id_provider | u | UNIQUE (generated_service_id, provider_id)
   ```

### **Option 2: Manual SQL (If Liquibase is Disabled)**

If you have `spring.liquibase.enabled=false`, run this SQL manually in pgAdmin:

```sql
-- Step 1: Drop global unique constraints
ALTER TABLE services DROP CONSTRAINT IF EXISTS services_service_code_key;
ALTER TABLE services DROP CONSTRAINT IF EXISTS services_generated_service_id_key;

-- Step 2: Add composite unique constraints
ALTER TABLE services ADD CONSTRAINT uk_services_code_provider 
    UNIQUE (service_code, provider_id);
    
ALTER TABLE services ADD CONSTRAINT uk_services_generated_id_provider 
    UNIQUE (generated_service_id, provider_id);

-- Step 3: Drop old index
DROP INDEX IF EXISTS idx_services_service_code;

-- Step 4: Create composite indexes
CREATE INDEX idx_services_code_provider 
    ON services (service_code, provider_id);
    
CREATE INDEX idx_services_generated_id_provider 
    ON services (generated_service_id, provider_id);

-- Step 5: Verify
SELECT conname, contype, pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conrelid = 'services'::regclass
  AND contype = 'u';
```

---

## 🧪 **Testing Scenarios**

### **Test Case 1: Same Service Code, Different Providers**

```java
// Provider A
Service serviceA = Service.builder()
    .serviceCode("SRV001")
    .serviceName("General Consultation")
    .provider(providerA)
    .build();
serviceRepository.save(serviceA);  // ✅ Success

// Provider B (different provider, same code)
Service serviceB = Service.builder()
    .serviceCode("SRV001")
    .serviceName("General Consultation")
    .provider(providerB)
    .build();
serviceRepository.save(serviceB);  // ✅ Success (different provider!)
```

### **Test Case 2: Same Service Code, Same Provider**

```java
// Provider A
Service service1 = Service.builder()
    .serviceCode("SRV001")
    .serviceName("General Consultation")
    .provider(providerA)
    .build();
serviceRepository.save(service1);  // ✅ Success

// Provider A (same provider, same code)
Service service2 = Service.builder()
    .serviceCode("SRV001")
    .serviceName("X-Ray")
    .provider(providerA)
    .build();
serviceRepository.save(service2);  // ❌ Fails with duplicate key error
```

### **Test Case 3: Import Same Service List for Multiple Providers**

```java
// Import services.xlsx for Provider A
importServiceList("services.xlsx", "provider-a-uuid");  // ✅ Success

// Import same services.xlsx for Provider B
importServiceList("services.xlsx", "provider-b-uuid");  // ✅ Success (now works!)
```

---

## 💡 **Benefits**

### **For Providers:**
- ✅ Each provider can define their own service codes
- ✅ No conflicts with other providers' service codes
- ✅ Can import standard service lists without modification

### **For System:**
- ✅ Data integrity maintained per provider
- ✅ No accidental duplicates within same provider
- ✅ Better query performance with composite indexes

### **For Developers:**
- ✅ Clearer data model (services belong to providers)
- ✅ Easier to manage multi-tenant data
- ✅ Consistent with business logic

---

## 🔍 **Verification Queries**

### **Check Unique Constraints:**

```sql
SELECT 
    conname AS constraint_name,
    contype AS constraint_type,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint 
WHERE conrelid = 'services'::regclass
  AND contype = 'u'
ORDER BY conname;
```

**Expected Output:**
```
constraint_name                      | constraint_type | constraint_definition
-------------------------------------|-----------------|------------------------------------------
services_service_uuid_key            | u               | UNIQUE (service_uuid)
uk_services_code_provider            | u               | UNIQUE (service_code, provider_id)
uk_services_generated_id_provider    | u               | UNIQUE (generated_service_id, provider_id)
```

### **Check Indexes:**

```sql
SELECT 
    indexname,
    indexdef
FROM pg_indexes 
WHERE tablename = 'services'
  AND indexname LIKE '%code%' OR indexname LIKE '%generated%'
ORDER BY indexname;
```

**Expected Output:**
```
indexname                           | indexdef
------------------------------------|--------------------------------------------------
idx_services_code_provider          | CREATE INDEX ... ON services (service_code, provider_id)
idx_services_generated_id_provider  | CREATE INDEX ... ON services (generated_service_id, provider_id)
```

### **Test Duplicate Detection:**

```sql
-- This should work (different providers)
INSERT INTO services (service_uuid, service_code, service_name, provider_id, is_deleted)
VALUES 
    ('uuid-1', 'SRV001', 'Service 1', 1, false),
    ('uuid-2', 'SRV001', 'Service 1', 2, false);  -- ✅ Different provider

-- This should fail (same provider)
INSERT INTO services (service_uuid, service_code, service_name, provider_id, is_deleted)
VALUES 
    ('uuid-3', 'SRV001', 'Service 2', 1, false);  -- ❌ Duplicate for provider 1
```

---

## 📝 **Rollback Support**

If you need to rollback this change:

```bash
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

This will:
1. Drop composite unique constraints
2. Drop composite indexes
3. Restore global unique constraints
4. Restore original index

---

## ✅ **Summary**

| Aspect | Before | After |
|--------|--------|-------|
| **Service Code Uniqueness** | Global (all providers) | Per provider |
| **Generated ID Uniqueness** | Global (all providers) | Per provider |
| **Import Same List** | ❌ Fails for 2nd provider | ✅ Works for all providers |
| **Data Integrity** | Too restrictive | Correct per business logic |
| **Constraint Type** | Single column | Composite (code + provider) |
| **Index Type** | Single column | Composite (better performance) |

**Services are now correctly scoped to their provider!** 🎉

