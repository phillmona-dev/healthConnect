# Liquibase Non-Core Tables Reset

## Overview

This document describes the Liquibase changelog files created to drop and recreate non-core tables in the HealthConnect Provider database. This is useful for resetting data while preserving core system tables.

## Core Tables (Preserved)

The following tables are considered **core** and will **NOT** be dropped:

- `insured` - Insured persons data
- `payers` - Insurance payers/companies
- `role` - User roles
- `privilege` - System privileges
- `role_privilege` - Role-privilege mappings
- `app_user` - Application users
- `providers` - Healthcare providers

## Non-Core Tables (Dropped & Recreated)

All other tables will be dropped and recreated, including:

### Claims & Billing
- `claims` - Medical claims
- `claim_attachments` - Claim file attachments
- `claim_comments` - Claim comments/notes
- `claim_logs` - Claim audit logs
- `claim_payments` - Claim payment records
- `claim_items` - Individual claim line items
- `batch_records` - Claim batch processing
- `batch_logs` - Batch processing logs

### Medication Dispensing
- `medication_dispensing` - Dispensing records
- `medication_dispensing_items` - Individual dispensed items
- `failed_external_dispensing_logs` - Failed external API calls

### Contracts & Services
- `contract_headers` - Contract main records
- `contract_details` - Contract line items/services
- `servicelists` - Available services
- `drugs` - Drug/medication catalog

### Groups & Organization
- `employee_dependant_groups` - Employee/dependant groupings
- `dependant_groups` - Dependant-specific groups
- `employee_insured_groups` - Employee-insured groupings
- `contract_detail_employee_groups` - Contract-group mappings

### Package Management
- `package_categories` - Service package categories
- `package_category_limits` - Per-person package limits
- `package_category_usage` - Package usage tracking
- `service_category_mappings` - Service-to-category mappings

### Personal Information
- `dependants` - Dependant persons
- `benefit_balances` - Benefit balance tracking

### Services & Transactions
- `provided_services` - Services provided records
- `payment_transactions` - Payment processing

### User Management (Non-Core)
- `password_reset_tokens` - Password reset tokens
- `refresh_tokens` - JWT refresh tokens
- `profile_pictures` - User profile images

## Changelog Files

### 1. `10-drop-non-core-tables.xml`

This file contains the drop operations in the correct order:

```xml
<!-- Drop tables with foreign key dependencies first -->
<dropTable tableName="claim_attachments" cascadeConstraints="true"/>
<dropTable tableName="claim_comments" cascadeConstraints="true"/>
<!-- ... more dependent tables ... -->

<!-- Drop main tables -->
<dropTable tableName="claims" cascadeConstraints="true"/>
<dropTable tableName="contract_headers" cascadeConstraints="true"/>
<!-- ... more main tables ... -->

<!-- Drop sequences -->
<sql>DROP SEQUENCE IF EXISTS package_category_seq CASCADE;</sql>
<!-- ... more sequences ... -->

<!-- Drop indexes -->
<sql>DROP INDEX IF EXISTS idx_claim_payer_status CASCADE;</sql>
<!-- ... more indexes ... -->
```

**Key Features:**
- **Cascading drops**: Uses `cascadeConstraints="true"` to handle foreign keys
- **Dependency order**: Drops dependent tables before parent tables
- **Sequence cleanup**: Removes all sequences for non-core tables
- **Index cleanup**: Removes any remaining indexes

### 2. `11-recreate-non-core-tables.xml`

This file recreates all the dropped tables with their complete structure:

```xml
<!-- Create sequences first -->
<createSequence sequenceName="package_category_seq" startValue="1" incrementBy="1"/>

<!-- Create tables with full column definitions -->
<createTable tableName="package_categories">
    <column name="id" type="BIGINT" autoIncrement="true">
        <constraints primaryKey="true" nullable="false"/>
    </column>
    <!-- ... more columns ... -->
</createTable>

<!-- Add foreign key constraints -->
<addForeignKeyConstraint baseTableName="servicelists" baseColumnNames="provider_id"
                         constraintName="fk_servicelist_provider"
                         referencedTableName="providers" referencedColumnNames="id"/>

<!-- Create indexes -->
<createIndex tableName="servicelists" indexName="idx_servicelist_provider">
    <column name="provider_id"/>
</createIndex>
```

**Key Features:**
- **Complete table structure**: All columns, constraints, and data types
- **Foreign key relationships**: Proper relationships to core tables
- **Indexes**: Performance-optimized indexes
- **Default values**: Appropriate default values for columns
- **Audit fields**: Created/modified date and user tracking

## Usage Instructions

### 1. **Backup Database** (CRITICAL!)
```bash
pg_dump -h localhost -U postgres -d healthConnect > backup_before_reset.sql
```

### 2. **Run Liquibase Update**
```bash
mvn liquibase:update
```

### 3. **Verify Results**
```sql
-- Check that core tables still exist with data
SELECT COUNT(*) FROM insured;
SELECT COUNT(*) FROM payers;
SELECT COUNT(*) FROM app_user;

-- Check that non-core tables are empty but exist
SELECT COUNT(*) FROM claims;
SELECT COUNT(*) FROM medication_dispensing;
SELECT COUNT(*) FROM contract_headers;
```

## What Happens During Execution

### Phase 1: Drop Non-Core Tables
1. **Foreign key dependent tables** are dropped first
2. **Main tables** are dropped next
3. **Sequences** are removed
4. **Indexes** are cleaned up

### Phase 2: Recreate Tables
1. **Sequences** are created
2. **Main tables** are created with full structure
3. **Foreign key constraints** are added
4. **Indexes** are created for performance

## Data Impact

### ✅ **Data Preserved**
- All insured persons and their basic information
- All payers/insurance companies
- All healthcare providers
- All users, roles, and privileges
- User authentication and authorization data

### ❌ **Data Lost**
- All claims and billing history
- All medication dispensing records
- All contracts and service agreements
- All package usage and limits
- All dependant information
- All groups and organizational structures

## Recovery Considerations

### **Before Running**
1. **Full database backup** is essential
2. **Export critical data** if needed for reimport
3. **Notify users** of the data reset
4. **Plan for data re-entry** or migration

### **After Running**
1. **Verify core data integrity**
2. **Re-create essential contracts** and services
3. **Re-import dependant data** if available
4. **Reconfigure package categories** and limits
5. **Test system functionality** thoroughly

## Rollback Strategy

If issues occur, you can rollback using:

```bash
# Restore from backup
psql -h localhost -U postgres -d healthConnect < backup_before_reset.sql

# Or use Liquibase rollback (if supported)
mvn liquibase:rollback -Dliquibase.rollbackCount=2
```

## Common Use Cases

### 1. **Development Environment Reset**
- Clean slate for testing
- Remove test data while keeping configuration
- Reset to known state

### 2. **Data Migration Preparation**
- Clear old data before importing new
- Restructure data organization
- Clean up corrupted data

### 3. **System Upgrade**
- Reset before major version upgrade
- Clean up legacy data structures
- Prepare for new features

## Monitoring & Validation

### **During Execution**
```bash
# Monitor Liquibase progress
tail -f target/liquibase.log

# Check database connections
psql -h localhost -U postgres -d healthConnect -c "SELECT version();"
```

### **After Execution**
```sql
-- Verify table structure
\dt

-- Check foreign key constraints
SELECT conname, conrelid::regclass, confrelid::regclass 
FROM pg_constraint 
WHERE contype = 'f';

-- Verify sequences
\ds

-- Check indexes
\di
```

## Troubleshooting

### **Common Issues**

1. **Foreign Key Constraint Errors**
   - Ensure drop order is correct
   - Use `cascadeConstraints="true"`

2. **Sequence Already Exists**
   - Add `IF EXISTS` to drop statements
   - Check for sequence dependencies

3. **Permission Errors**
   - Ensure database user has DROP/CREATE privileges
   - Check table ownership

4. **Timeout Issues**
   - Increase Liquibase timeout settings
   - Run during low-usage periods

### **Validation Queries**
```sql
-- Check for missing tables
SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;

-- Verify foreign keys are working
SELECT * FROM information_schema.table_constraints WHERE constraint_type = 'FOREIGN KEY';

-- Check sequence ownership
SELECT schemaname, sequencename, sequenceowner FROM pg_sequences;
```

This reset process provides a clean way to refresh non-core data while preserving essential system configuration and user data.
