# Services Table Setup Guide

## What Was Done

### 1. Removed All Old Changelogs ✅
All previous changelog files have been deleted:
- `00-recreate-complete-schema.xml`
- `10-drop-non-core-tables.xml`
- `11-recreate-non-core-tables.xml`
- `12-add-is-cbhi-column-simple.xml`
- `13-complete-medication-dispensing-table.xml`
- `13-simple-medication-dispensing-fix.xml`
- `14-create-contract-details-table.xml`
- `14-simple-contract-details-table.xml`
- `15-fix-contract-details-junction-table.xml`
- `16-fix-attachment-data-column-type.xml`
- `17-fix-failed-external-dispensing-log-status.xml`
- `17-make-provider-uuid-nullable-package-categories.xml`
- `18-create-failed-external-claim-log.xml`
- `19-create-dispensing-rejection-table.xml`
- `fix-tin-number-constraint.xml`

### 2. Created New Services Table Changelog ✅
Created: `src/main/resources/db/changelog/01-create-services-table.xml`

This changelog creates the `services` table with:
- **Primary Key**: `id` (BIGINT, auto-increment)
- **UUID Fields**: `service_uuid` (unique, not null)
- **Identifiers**: `generated_service_id`, `service_code` (unique)
- **Service Info**: `service_name`, `service_description`, `service_category`, `service_sub_category`
- **Pricing**: `default_price`, `negotiated_price`, `price`
- **Other**: `unit_of_measure`, `status`
- **Foreign Key**: `provider_id` → `providers(id)`
- **Soft Delete**: `is_deleted` (default false)
- **Audit Fields**: `created_by`, `updated_by`, `created_at`, `updated_at`

### 3. Updated Master Changelog ✅
Updated: `src/main/resources/db/changelog/db.changelog-master.xml`

Now only includes:
```xml
<include file="db/changelog/01-create-services-table.xml" relativeToChangelogFile="false"/>
```

### 4. Created Reset Script ✅
Created: `reset-liquibase-for-services.sql`

This script clears Liquibase tracking so it can run the new changelog.

## How to Apply

### Step 1: Reset Liquibase Tracking

Open **pgAdmin** and run `reset-liquibase-for-services.sql`:

```sql
-- Clear all Liquibase tracking
TRUNCATE TABLE databasechangelog;
TRUNCATE TABLE databasechangeloglock;
```

### Step 2: Verify Liquibase is Enabled

Check `src/main/resources/application.properties`:

```properties
spring.liquibase.enabled=true  # ✅ Should be true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml
```

### Step 3: Restart Your Application

```bash
# Stop current application (Ctrl+C)
mvn spring-boot:run
```

### Step 4: Verify Table Creation

Check the logs - you should see:

```
INFO - Liquibase: Running Changeset: db/changelog/01-create-services-table.xml::create-services-table::system
INFO - Liquibase: Table services created
```

Verify in pgAdmin:

```sql
-- Check if table exists
SELECT * FROM services LIMIT 1;

-- Check table structure
\d services
```

## Table Structure

```sql
CREATE TABLE services (
    id BIGSERIAL PRIMARY KEY,
    service_uuid VARCHAR(255) NOT NULL UNIQUE,
    generated_service_id VARCHAR(255) UNIQUE,
    service_code VARCHAR(255) UNIQUE,
    service_name VARCHAR(255) NOT NULL,
    service_description TEXT,
    service_category VARCHAR(255),
    service_sub_category VARCHAR(255),
    default_price DOUBLE PRECISION,
    negotiated_price DOUBLE PRECISION,
    price DOUBLE PRECISION,
    unit_of_measure VARCHAR(100),
    status VARCHAR(50),
    provider_id BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_services_provider FOREIGN KEY (provider_id) 
        REFERENCES providers(id) ON DELETE SET NULL ON UPDATE CASCADE
);
```

## Indexes Created

- `idx_services_service_uuid` on `service_uuid`
- `idx_services_service_code` on `service_code`
- `idx_services_service_name` on `service_name`
- `idx_services_provider_id` on `provider_id`
- `idx_services_status` on `status`
- `idx_services_is_deleted` on `is_deleted`
- `idx_services_category` on `service_category`

## Features

### Preconditions
The changelog includes a precondition that checks if the table already exists:
```xml
<preConditions onFail="MARK_RAN">
    <not>
        <tableExists tableName="services"/>
    </not>
</preConditions>
```

If the table exists, Liquibase will mark the changeset as ran without executing it.

### Rollback Support
The changelog includes rollback support:
```xml
<rollback>
    <dropTable tableName="services" cascadeConstraints="true"/>
</rollback>
```

You can rollback using:
```bash
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

## Troubleshooting

### Error: "Table already exists"

If you get this error, the table exists but Liquibase doesn't know about it:

```sql
-- Option 1: Drop the table
DROP TABLE IF EXISTS services CASCADE;

-- Option 2: Mark changeset as executed without running it
INSERT INTO databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum)
VALUES ('create-services-table', 'system', 'db/changelog/01-create-services-table.xml', NOW(), 1, 'EXECUTED', '8:...');
```

### Error: "Foreign key constraint fails"

The `providers` table must exist before creating `services`:

```sql
-- Check if providers table exists
SELECT * FROM providers LIMIT 1;
```

If it doesn't exist, you need to create it first or remove the foreign key constraint from the changelog.

## Next Steps

After the services table is created:

1. ✅ Test creating a service record
2. ✅ Verify foreign key to providers works
3. ✅ Add more changelogs as needed for other tables
4. ✅ Keep Liquibase enabled for future schema changes

## Files Created

- `src/main/resources/db/changelog/01-create-services-table.xml` - Services table changelog
- `reset-liquibase-for-services.sql` - Liquibase reset script
- `SERVICES_TABLE_SETUP.md` - This guide

