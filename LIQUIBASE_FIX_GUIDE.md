# Liquibase Fix Guide - Missing Tables

## Problem
After manually dropping tables and trying to use Hibernate, some tables are not being created properly due to foreign key dependencies:
- `services` table is missing
- `employee_dependant_groups` table has data issues
- Foreign key constraints are failing

## Solution: Use Liquibase to Recreate Missing Tables

### Step 1: Stop Your Application
```bash
# Press Ctrl+C to stop the running application
```

### Step 2: Run the Fix SQL Script in pgAdmin

Open pgAdmin and run the `fix-missing-tables.sql` script:

```sql
-- This script will:
-- 1. Check which tables are missing
-- 2. Remove Liquibase tracking for those tables
-- 3. Fix foreign key constraint issues
-- 4. Allow Liquibase to recreate the missing tables
```

**OR** if you want a complete fresh start:

```sql
-- Complete reset (use with caution)
TRUNCATE TABLE databasechangelog;
TRUNCATE TABLE databasechangeloglock;
```

### Step 3: Verify Liquibase is Enabled

Check `src/main/resources/application.properties`:

```properties
# Should be TRUE
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml

# Hibernate should be on 'validate' or 'none' when using Liquibase
spring.jpa.hibernate.ddl-auto=validate
```

### Step 4: Restart Your Application

```bash
mvn spring-boot:run
```

### Step 5: Check the Logs

You should see Liquibase running changesets:

```
INFO  - Liquibase: Running Changeset: db/changelog/11-recreate-non-core-tables.xml::recreate-servicelists-table::system
INFO  - Liquibase: Running Changeset: db/changelog/11-recreate-non-core-tables.xml::recreate-employee-dependant-groups-table::system
INFO  - Liquibase: Running Changeset: db/changelog/18-create-failed-external-claim-log.xml::create-failed-external-claim-log-table::system
```

## What Each File Does

### `fix-missing-tables.sql`
- Checks which tables are missing
- Removes Liquibase tracking for missing tables
- Fixes foreign key constraint issues
- Allows Liquibase to recreate only the missing tables

### `fix-liquibase-tracking.sql`
- More aggressive approach
- Deletes all changesets related to specific tables
- Use if the first script doesn't work

## Troubleshooting

### If Liquibase Still Fails

**Error: "relation already exists"**
```sql
-- Drop the specific table that exists
DROP TABLE IF EXISTS failed_external_claim_log CASCADE;

-- Then remove its changeset tracking
DELETE FROM databasechangelog WHERE id = 'create-failed-external-claim-log-table';
```

**Error: "foreign key constraint violation"**
```sql
-- Fix invalid foreign key references
UPDATE insured SET group_id = NULL 
WHERE group_id NOT IN (SELECT id FROM employee_dependant_groups);

UPDATE contract_details SET service_id = NULL 
WHERE service_id NOT IN (SELECT id FROM servicelists);
```

### If You Want to Start Completely Fresh

```sql
-- 1. Backup your data first!
CREATE TABLE backup_insured AS SELECT * FROM insured;
CREATE TABLE backup_providers AS SELECT * FROM providers;
CREATE TABLE backup_payers AS SELECT * FROM payers;
-- ... backup other important tables

-- 2. Drop all tables
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;

-- 3. Restart application - Liquibase will create everything from scratch
```

## Recommended Settings

### For Development (with Liquibase):
```properties
spring.liquibase.enabled=true
spring.jpa.hibernate.ddl-auto=validate
```

### For Development (without Liquibase):
```properties
spring.liquibase.enabled=false
spring.jpa.hibernate.ddl-auto=update
```

### For Production:
```properties
spring.liquibase.enabled=true
spring.jpa.hibernate.ddl-auto=none
```

## Next Steps

1. ✅ Run `fix-missing-tables.sql` in pgAdmin
2. ✅ Verify `spring.liquibase.enabled=true` in application.properties
3. ✅ Restart your application
4. ✅ Check logs for successful table creation
5. ✅ Test your API endpoints

## Files Created

- `fix-missing-tables.sql` - Main fix script (recommended)
- `fix-liquibase-tracking.sql` - Alternative fix script
- `LIQUIBASE_FIX_GUIDE.md` - This guide

