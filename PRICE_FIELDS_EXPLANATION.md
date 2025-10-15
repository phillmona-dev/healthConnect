# Price Fields Explanation: defaultPrice vs negotiatedPrice vs price

## 📊 Overview

Your system has **THREE** price fields that serve different purposes in the pricing hierarchy:

| Field | Location | Purpose | Who Sets It | When Used |
|-------|----------|---------|-------------|-----------|
| **`defaultPrice`** | `Servicelist` entity | Base/catalog price | Provider | Fallback when no contract exists |
| **`negotiatedPrice`** | `ContractDetail` entity | Contract-specific price | Payer + Provider | When contract exists |
| **`price`** | `Servicelist` entity | Current/display price | System | UI display purposes |

---

## 🏗️ Entity Structure

### 1. **Servicelist Entity** (Service Catalog)

<augment_code_snippet path="src/main/java/com/medco/HealthConnectProvider/entity/services/Servicelist.java" mode="EXCERPT">
````java
@Entity
@Table(name = "services")
public class Servicelist extends Audit {
    
    @Column(precision = 19)
    private Double defaultPrice;      // ← Base catalog price
    
    private Double negotiatedPrice;   // ⚠️ DEPRECATED - not used
    
    private Double price;             // ← Display price (UI)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;
    
    @OneToMany(mappedBy = "servicelist")
    private List<ContractDetail> contractDetails;
}
````
</augment_code_snippet>

### 2. **ContractDetail Entity** (Contract-Specific Pricing)

<augment_code_snippet path="src/main/java/com/medco/HealthConnectProvider/entity/contracts/ContractDetail.java" mode="EXCERPT">
````java
@Entity
@Table(name = "contract_details")
public class ContractDetail extends Audit {
    
    @Column(precision = 19)
    @Builder.Default
    private Double negotiatedPrice = 0.0;  // ← Contract price (ACTUAL)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Servicelist servicelist;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_header_id")
    private ContractHeader contractHeader;
}
````
</augment_code_snippet>

---

## 💰 Price Field Details

### 1️⃣ **`defaultPrice`** (Servicelist)

**Purpose:** Base catalog price set by the provider

**Characteristics:**
- ✅ Set when service is created
- ✅ Same for all payers (unless contracted)
- ✅ Used as fallback when no contract exists
- ✅ Provider's standard rate

**Example:**
```java
Servicelist service = new Servicelist();
service.setServiceName("X-Ray Chest");
service.setDefaultPrice(500.0);  // ← Provider's standard price
```

**When Used:**
- Service not covered by contract
- No negotiated price available
- Displaying service catalog

---

### 2️⃣ **`negotiatedPrice`** (ContractDetail)

**Purpose:** Contract-specific price negotiated between payer and provider

**Characteristics:**
- ✅ Set when contract is created
- ✅ Different for each payer
- ✅ **ALWAYS USED** when contract exists
- ✅ Overrides `defaultPrice`
- ✅ Can be lower or higher than `defaultPrice`

**Example:**
```java
ContractDetail contractDetail = new ContractDetail();
contractDetail.setServiceUuid("service-123");
contractDetail.setNegotiatedPrice(400.0);  // ← Payer negotiated lower price
contractDetail.setContractHeader(contract);
```

**When Used:**
- ✅ Eligibility checks
- ✅ Claim calculations
- ✅ Medication dispensing
- ✅ Coverage calculations

---

### 3️⃣ **`price`** (Servicelist)

**Purpose:** Current/display price for UI purposes

**Characteristics:**
- ✅ Used for display in service lists
- ✅ Falls back to `defaultPrice` if not set
- ✅ Not used in actual billing calculations

**Example:**
```java
// In ServicelistServiceImpl.java
if (serviceEntity.getPrice() != null) {
    response.setPrice(BigDecimal.valueOf(serviceEntity.getPrice()));
} else if (serviceEntity.getDefaultPrice() != null) {
    response.setPrice(BigDecimal.valueOf(serviceEntity.getDefaultPrice()));
} else {
    response.setPrice(BigDecimal.valueOf(0.0));
}
```

---

## 🔄 Pricing Flow

### Scenario 1: **Service WITH Contract** (Most Common)

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Patient requests service                                 │
│ 2. System checks eligibility                                │
│ 3. Finds contract between Provider & Payer                  │
│ 4. Looks up ContractDetail for service                      │
│ 5. Uses negotiatedPrice from ContractDetail ✅              │
│ 6. Calculates co-payment based on negotiatedPrice           │
└─────────────────────────────────────────────────────────────┘
```

**Code Example:**
<augment_code_snippet path="src/main/java/com/medco/HealthConnectProvider/services/impl/eligibility/EligibilityServiceImpl.java" mode="EXCERPT">
````java
private ServiceEligibilityResponse calculateCoverage(
        ServiceEligibilityResponse response,
        ContractDetail bestContractDetail,
        ContractHeader contractHeader,
        Insured insured) {
    
    response.setCovered(true);
    
    // ✅ Uses negotiatedPrice from ContractDetail
    response.setPrice(BigDecimal.valueOf(bestContractDetail.getNegotiatedPrice()));
    
    Double coPaymentPercentage = contractHeader.getCoPaymentPercentage() != null ?
            contractHeader.getCoPaymentPercentage() : 20.0;
    
    response.setCoPaymentPercentage(coPaymentPercentage);
    
    BigDecimal price = BigDecimal.valueOf(bestContractDetail.getNegotiatedPrice());
    BigDecimal coPaymentAmount = price.multiply(BigDecimal.valueOf(coPaymentPercentage / 100.0));
    
    response.setCoPaymentAmount(coPaymentAmount);
    response.setInsuranceCoverage(price.subtract(coPaymentAmount));
    
    return response;
}
````
</augment_code_snippet>

**Example:**
```
Service: X-Ray Chest
defaultPrice: 500.00 ETB (in Servicelist)
negotiatedPrice: 400.00 ETB (in ContractDetail)

✅ USED: 400.00 ETB (negotiatedPrice)
❌ NOT USED: 500.00 ETB (defaultPrice)

Co-payment (20%): 80.00 ETB
Insurance Coverage: 320.00 ETB
Patient Pays: 80.00 ETB
```

---

### Scenario 2: **Service WITHOUT Contract** (Uncovered)

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Patient requests service                                 │
│ 2. System checks eligibility                                │
│ 3. No contract found for this service                       │
│ 4. Uses defaultPrice from Servicelist ✅                    │
│ 5. Patient pays 100% (not covered)                          │
└─────────────────────────────────────────────────────────────┘
```

**Code Example:**
<augment_code_snippet path="src/main/java/com/medco/HealthConnectProvider/services/impl/eligibility/EligibilityServiceImpl.java" mode="EXCERPT">
````java
private ServiceEligibilityResponse handleUncoveredService(
        ServiceEligibilityResponse response, 
        Servicelist service) {
    
    response.setCovered(false);
    
    // ✅ Uses price from Servicelist (falls back to defaultPrice)
    response.setPrice(BigDecimal.valueOf(service.getPrice()));
    response.setCoPaymentAmount(BigDecimal.valueOf(service.getPrice()));
    response.setCoPaymentPercentage(100.0);  // Patient pays 100%
    response.setInsuranceCoverage(BigDecimal.valueOf(0.0));
    
    return response;
}
````
</augment_code_snippet>

**Example:**
```
Service: Cosmetic Surgery
defaultPrice: 10,000.00 ETB (in Servicelist)
negotiatedPrice: N/A (no contract)

✅ USED: 10,000.00 ETB (defaultPrice)
❌ NOT COVERED by insurance

Co-payment (100%): 10,000.00 ETB
Insurance Coverage: 0.00 ETB
Patient Pays: 10,000.00 ETB (full amount)
```

---

## 📋 Real-World Examples

### Example 1: Multiple Payers, Different Prices

**Service:** Blood Test (CBC)

| Payer | Contract? | Price Used | Source |
|-------|-----------|------------|--------|
| **No Insurance** | ❌ No | 200.00 ETB | `defaultPrice` |
| **Payer A** | ✅ Yes | 180.00 ETB | `negotiatedPrice` (10% discount) |
| **Payer B** | ✅ Yes | 150.00 ETB | `negotiatedPrice` (25% discount) |
| **Payer C** | ✅ Yes | 200.00 ETB | `negotiatedPrice` (same as default) |

**Database:**
```sql
-- Servicelist table
INSERT INTO services (service_name, default_price, price)
VALUES ('Blood Test (CBC)', 200.00, 200.00);

-- ContractDetail table
INSERT INTO contract_details (service_uuid, contract_header_uuid, negotiated_price)
VALUES 
  ('service-123', 'contract-payer-a', 180.00),  -- Payer A
  ('service-123', 'contract-payer-b', 150.00),  -- Payer B
  ('service-123', 'contract-payer-c', 200.00);  -- Payer C
```

---

### Example 2: Pharmacy Dispensing

**Medication:** Amoxicillin 500mg

```java
// 1. Check eligibility
EligibilityResponse eligibility = checkEligibility(insuredUuid, drugUuid);

// 2. Get negotiated price from contract
ContractDetail contractDetail = findContractDetail(contractUuid, drugUuid);
Double negotiatedPrice = contractDetail.getNegotiatedPrice();  // 50.00 ETB

// 3. Calculate totals
MedicationDispensingItem item = new MedicationDispensingItem();
item.setUnitPrice(negotiatedPrice);  // ✅ 50.00 ETB
item.setQuantity(10.0);
item.setTotalPrice(negotiatedPrice * 10.0);  // 500.00 ETB

// 4. Calculate coverage
Double coPaymentPercentage = 20.0;
Double insuranceCoverage = 500.00 * 0.80;  // 400.00 ETB
Double patientResponsibility = 500.00 * 0.20;  // 100.00 ETB
```

**Result:**
```
Drug: Amoxicillin 500mg
Quantity: 10 tablets
Unit Price: 50.00 ETB (negotiatedPrice)
Total: 500.00 ETB

Insurance Pays: 400.00 ETB (80%)
Patient Pays: 100.00 ETB (20%)
```

---

## 🎯 Key Differences Summary

| Aspect | `defaultPrice` | `negotiatedPrice` | `price` |
|--------|----------------|-------------------|---------|
| **Entity** | Servicelist | ContractDetail | Servicelist |
| **Set By** | Provider | Payer + Provider | System |
| **Scope** | Global (all payers) | Per contract | Display only |
| **Used In Billing** | ✅ Yes (fallback) | ✅ Yes (primary) | ❌ No |
| **Can Vary By Payer** | ❌ No | ✅ Yes | ❌ No |
| **Overrides** | Nothing | `defaultPrice` | Nothing |
| **Priority** | Low | **High** | N/A |

---

## 🔍 How to Determine Which Price is Used

### Decision Tree:

```
Is there a contract between Provider & Payer?
│
├─ YES → Does contract include this service?
│         │
│         ├─ YES → Use negotiatedPrice from ContractDetail ✅
│         │
│         └─ NO → Use defaultPrice from Servicelist ✅
│
└─ NO → Use defaultPrice from Servicelist ✅
```

---

## 💡 Best Practices

### 1. **Always Set `defaultPrice`**
```java
// ✅ GOOD
Servicelist service = new Servicelist();
service.setDefaultPrice(500.0);  // Always set base price

// ❌ BAD
Servicelist service = new Servicelist();
// defaultPrice is null - will cause issues
```

### 2. **Always Set `negotiatedPrice` in Contracts**
```java
// ✅ GOOD
ContractDetail detail = new ContractDetail();
detail.setNegotiatedPrice(400.0);  // Explicit price

// ❌ BAD
ContractDetail detail = new ContractDetail();
// negotiatedPrice defaults to 0.0 - patient gets free service!
```

### 3. **Validate Prices**
```java
// ✅ GOOD
if (contractDetail.getNegotiatedPrice() == null || 
    contractDetail.getNegotiatedPrice() <= 0) {
    throw new ValidationException("Negotiated price must be greater than 0");
}

// ✅ GOOD
if (service.getDefaultPrice() == null || 
    service.getDefaultPrice() <= 0) {
    throw new ValidationException("Default price must be greater than 0");
}
```

---

## 🚨 Common Issues

### Issue 1: **Negotiated Price is 0.0**
```
Problem: Patient gets service for free
Cause: negotiatedPrice defaults to 0.0 in @PrePersist
Solution: Always set explicit price when creating contract
```

### Issue 2: **Default Price is NULL**
```
Problem: Uncovered services show 0.00 ETB
Cause: defaultPrice not set when creating service
Solution: Validate defaultPrice is set and > 0
```

### Issue 3: **Wrong Price Used**
```
Problem: System uses defaultPrice instead of negotiatedPrice
Cause: Contract exists but service not in ContractDetail
Solution: Add service to contract with negotiatedPrice
```

---

## 📊 Database Queries

### Check Prices for a Service
```sql
-- Get all prices for a service
SELECT 
    s.service_name,
    s.default_price,
    s.price,
    ch.payer_uuid,
    cd.negotiated_price,
    CASE 
        WHEN cd.negotiated_price IS NOT NULL THEN cd.negotiated_price
        ELSE s.default_price
    END as effective_price
FROM services s
LEFT JOIN contract_details cd ON s.service_uuid = cd.service_uuid
LEFT JOIN contract_headers ch ON cd.contract_header_uuid = ch.contract_header_uuid
WHERE s.service_uuid = 'service-123'
ORDER BY ch.payer_uuid;
```

### Find Services with Missing Prices
```sql
-- Services without defaultPrice
SELECT service_uuid, service_name
FROM services
WHERE default_price IS NULL OR default_price <= 0;

-- Contract details without negotiatedPrice
SELECT cd.contract_detail_uuid, s.service_name, ch.payer_uuid
FROM contract_details cd
JOIN services s ON cd.service_uuid = s.service_uuid
JOIN contract_headers ch ON cd.contract_header_uuid = ch.contract_header_uuid
WHERE cd.negotiated_price IS NULL OR cd.negotiated_price <= 0;
```

---

## ✅ Summary

| When | Use This Price | From |
|------|----------------|------|
| **Service covered by contract** | `negotiatedPrice` | `ContractDetail` |
| **Service NOT covered** | `defaultPrice` | `Servicelist` |
| **Displaying service catalog** | `price` (or `defaultPrice`) | `Servicelist` |
| **Eligibility check (covered)** | `negotiatedPrice` | `ContractDetail` |
| **Eligibility check (uncovered)** | `defaultPrice` | `Servicelist` |
| **Claim calculation** | `negotiatedPrice` | `ContractDetail` |
| **Pharmacy dispensing** | `negotiatedPrice` | `ContractDetail` |

**Bottom Line:**
- **`negotiatedPrice`** = The ACTUAL price used for billing when contract exists ✅
- **`defaultPrice`** = The FALLBACK price when no contract exists ✅
- **`price`** = Display price for UI (not used in calculations) ℹ️

**Priority:** `negotiatedPrice` > `defaultPrice` > `price`

