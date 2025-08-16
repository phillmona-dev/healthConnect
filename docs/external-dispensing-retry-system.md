# External Dispensing Retry System

## Overview

The External Dispensing Retry System automatically handles failed external insurance system calls by logging failures and retrying them on a scheduled basis. This ensures that temporary network issues or external system downtime don't result in lost dispensing data.

## Key Features

### 1. **Automatic Failure Logging** 📝
- Logs all failed external API calls
- Stores complete request details for retry
- Tracks retry attempts and error messages
- Maintains audit trail of all attempts

### 2. **Scheduled Retry Processing** ⏰
- **Daily at 12:00 PM**: Main retry processing
- **Every 4 hours**: Optional frequent retries (configurable)
- **Daily at 2:00 AM**: Cleanup old logs (configurable)

### 3. **Exponential Backoff** 📈
- 1st retry: 1 hour after failure
- 2nd retry: 2 hours after previous attempt
- 3rd retry: 4 hours after previous attempt
- 4th retry: 8 hours after previous attempt
- 5th retry: 16 hours after previous attempt
- Maximum: 24 hours between retries

### 4. **Management APIs** 🔧
- View pending retries
- Manual retry triggers
- Statistics and monitoring
- Status management

## Database Schema

### FailedExternalDispensingLog Table

```sql
CREATE TABLE failed_external_dispensing_log (
    id BIGSERIAL PRIMARY KEY,
    log_uuid VARCHAR(255) UNIQUE NOT NULL,
    dispensing_item_uuid VARCHAR(255) NOT NULL,
    dispensing_uuid VARCHAR(255) NOT NULL,
    service_id VARCHAR(255) NOT NULL,
    insured_uuid VARCHAR(255) NOT NULL,
    package_uuid VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    total_price DOUBLE PRECISION NOT NULL,
    provided_date VARCHAR(255) NOT NULL,
    provider_uuid VARCHAR(255) NOT NULL,
    external_api_url TEXT,
    error_message TEXT,
    last_response TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 5,
    next_retry_at TIMESTAMP NOT NULL,
    first_failed_at TIMESTAMP NOT NULL,
    last_attempt_at TIMESTAMP,
    succeeded_at TIMESTAMP,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);
```

## Configuration

### Application Properties

```properties
# External API Configuration
external.api.external-api-base-url=http://192.168.100.85:8888
external.api.api-key=hc_7f9a3b2e4d5c1f8e6a0d9b7c5e3f1a2d
external.api.enabled=true

# Retry Configuration
external.api.retry.scheduler.enabled=true
external.api.retry.frequent.enabled=false
external.api.retry.cleanup.enabled=true

# Spring Scheduling
spring.task.scheduling.pool.size=5
```

### Configuration Options

| Property | Default | Description |
|----------|---------|-------------|
| `external.api.retry.scheduler.enabled` | `true` | Enable/disable daily 12 PM retry processing |
| `external.api.retry.frequent.enabled` | `false` | Enable/disable 4-hourly retry processing |
| `external.api.retry.cleanup.enabled` | `true` | Enable/disable daily cleanup of old logs |
| `spring.task.scheduling.pool.size` | `5` | Number of threads for scheduled tasks |

## Scheduled Tasks

### 1. Main Retry Processing (Daily at 12:00 PM)

```java
@Scheduled(cron = "0 0 12 * * ?", zone = "Africa/Addis_Ababa")
public void processFailedExternalDispensingRetries()
```

**What it does:**
- Processes all pending retries that are due
- Updates retry counts and next retry times
- Marks successful retries as completed
- Marks max-retry-reached logs as inactive

### 2. Frequent Retry Processing (Every 4 Hours - Optional)

```java
@Scheduled(cron = "0 0 */4 * * ?", zone = "Africa/Addis_Ababa")
public void processFailedExternalDispensingRetriesFrequent()
```

**What it does:**
- More frequent processing for urgent retries
- Only processes retries that are due
- Disabled by default (enable via configuration)

### 3. Cleanup Task (Daily at 2:00 AM)

```java
@Scheduled(cron = "0 0 2 * * ?", zone = "Africa/Addis_Ababa")
public void cleanupOldRetryLogs()
```

**What it does:**
- Cleans up old successful retry logs
- Maintains logs for audit purposes (30 days)
- Prevents database bloat

## Management APIs

### 1. Get Pending Retries

```bash
GET /api/v1/healthConnect/failed-external-dispensing/pending-retries
Authorization: Bearer {your-jwt-token}
```

**Response:**
```json
[
  {
    "logUuid": "log-uuid-1",
    "dispensingItemUuid": "item-uuid-1",
    "serviceId": "SRV0000000001",
    "retryCount": 2,
    "nextRetryAt": "2024-01-15T14:00:00",
    "errorMessage": "Connection timeout",
    "status": "ACTIVE"
  }
]
```

### 2. Get Retry Statistics

```bash
GET /api/v1/healthConnect/failed-external-dispensing/statistics
Authorization: Bearer {your-jwt-token}
```

**Response:**
```json
{
  "pendingRetries": 15,
  "maxRetriesReached": 3,
  "successfulRetries": 127,
  "totalFailed": 145
}
```

### 3. Manual Retry

```bash
POST /api/v1/healthConnect/failed-external-dispensing/manual-retry/{logUuid}
Authorization: Bearer {your-jwt-token}
```

### 4. Process All Retries (Manual Trigger)

```bash
POST /api/v1/healthConnect/failed-external-dispensing/process-retries
Authorization: Bearer {your-jwt-token}
```

### 5. Get Failed Logs by Status

```bash
GET /api/v1/healthConnect/failed-external-dispensing/by-status/ACTIVE
Authorization: Bearer {your-jwt-token}
```

## Retry Logic Flow

### 1. Initial Failure
```
Dispensing API Call → External System Fails → Log Failure → Schedule Retry (1 hour)
```

### 2. Retry Process
```
Scheduler Runs → Check Due Retries → Attempt External Call → Update Status
```

### 3. Success Path
```
Retry Succeeds → Mark as COMPLETED → Remove from pending
```

### 4. Failure Path
```
Retry Fails → Increment Count → Schedule Next Retry (Exponential Backoff)
```

### 5. Max Retries Reached
```
5 Failures → Mark as INACTIVE → Stop Retrying → Manual Intervention Required
```

## Monitoring and Alerting

### Log Messages to Monitor

```
INFO  - Starting scheduled retry process for failed external dispensing
INFO  - Processing summary - Processed: 5, New Successful: 3, New Max Retries: 1
WARN  - Retry 3 failed for log: log-uuid. Next retry at: 2024-01-15T18:00:00
ERROR - Max retries reached for log: log-uuid
```

### Key Metrics to Track

1. **Pending Retries Count**: Should remain low
2. **Success Rate**: Percentage of successful retries
3. **Max Retries Reached**: Indicates persistent issues
4. **Processing Time**: How long retry processing takes

## Troubleshooting

### High Pending Retries

**Possible Causes:**
- External system is down
- Network connectivity issues
- Invalid API credentials
- Rate limiting by external system

**Solutions:**
- Check external system status
- Verify network connectivity
- Validate API credentials
- Review rate limiting policies

### Max Retries Reached

**Possible Causes:**
- Persistent external system issues
- Invalid data in retry logs
- Configuration problems

**Solutions:**
- Investigate error messages in logs
- Manual retry after fixing issues
- Update configuration if needed

### Performance Issues

**Possible Causes:**
- Too many pending retries
- Insufficient thread pool size
- Database performance

**Solutions:**
- Increase thread pool size
- Optimize database queries
- Consider batch processing

## Best Practices

1. **Monitor Regularly**: Check retry statistics daily
2. **Set Alerts**: Alert on high pending retry counts
3. **Review Failures**: Investigate max-retry-reached logs
4. **Maintain Logs**: Regular cleanup of old successful logs
5. **Test Connectivity**: Regular health checks of external system
6. **Backup Strategy**: Ensure retry logs are included in backups

## Security Considerations

1. **API Keys**: Secure storage of external API credentials
2. **Access Control**: Restrict access to retry management APIs
3. **Audit Trail**: Maintain logs of manual retry operations
4. **Data Privacy**: Ensure retry logs don't expose sensitive data
