# HealthConnect Provider

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Containerized-blue.svg)](https://www.docker.com/)

## Overview

HealthConnect Provider is a comprehensive healthcare management system designed to facilitate seamless integration 
between healthcare providers, insurance payers, and pharmacy systems. The application serves as a central hub for managing insurance claims, 
eligibility verification, medication dispensing, and provider-payer relationships.

## Key Features

### 🏥 Provider Management
- Healthcare provider registration and profile management
- Provider-payer contract management
- Service catalog management
- Dashboard analytics and reporting

### 💊 Pharmacy Integration
- Real-time medication dispensing recording
- Integration with external pharmacy systems (Kenema Pharmacy Management System)
- Prescription tracking and management
- Automated claim generation from dispensing records

### 🔍 Eligibility Verification
- Real-time insurance eligibility checking
- Patient coverage verification
- Service-specific eligibility validation
- Group membership verification

### 📦 Package Category Management
- Configurable service package categories (Inpatient, Outpatient, Dental, etc.)
- Per-person limits configuration by service providers
- Contract-based category limit management
- Real-time usage tracking and limit consumption
- Multi-category service mapping support

### 📋 Claims Management
- Automated claim creation and submission
- Claim status tracking and updates
- Batch claim processing
- Payment reconciliation
- Claim approval/rejection workflows

### 👥 User & Access Management
- Role-based access control (RBAC)
- JWT-based authentication
- API key authentication for integrations
- User registration and profile management

### 💳 Payment Processing
- Integration with Chapa payment gateway
- Payment transaction tracking
- Financial reconciliation
- Payment status management

## Technology Stack

### Backend Framework
- **Java 17** - Programming language
- **Spring Boot 3.5.0** - Application framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Data persistence
- **Spring Web** - REST API development

### Database & Caching
- **PostgreSQL** - Primary database
- **Redis** - Caching and session management
- **Liquibase** - Database migration and versioning

### Integration & Messaging
- **RabbitMQ** - Message queuing (optional)
- **Spring WebFlux** - Reactive web client
- **JWT** - Token-based authentication

### Documentation & API
- **Swagger/OpenAPI 3** - API documentation
- **Spring Boot Actuator** - Application monitoring

### Additional Libraries
- **Lombok** - Code generation
- **ModelMapper** - Object mapping
- **Apache POI** - Excel file processing
- **Jackson** - JSON processing

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL 12+
- Redis (optional, for caching)
- RabbitMQ (optional, for messaging)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd HealthConnectProvider
   ```

2. **Database Setup**
   ```sql
   CREATE DATABASE healthConnect;
   CREATE USER postgres WITH PASSWORD 'postgres';
   GRANT ALL PRIVILEGES ON DATABASE healthConnect TO postgres;
   ```

3. **Configure Application Properties**
   
   Copy and modify the configuration files:
   ```bash
   cp src/main/resources/application-dev.properties.example src/main/resources/application-dev.properties
   ```
   
   Update database credentials and other configurations as needed.

4. **Build the Application**
   ```bash
   mvn clean install
   ```

5. **Run the Application**
   ```bash
   mvn spring-boot:run
   ```

   Or run the JAR file:
   ```bash
   java -jar target/HealthConnectProvider-0.0.1-SNAPSHOT.jar
   ```

### Docker Deployment

1. **Build Docker Image**
   ```bash
   docker build -t healthconnect-provider .
   ```

2. **Run with Docker**
   ```bash
   docker run -p 3012:3012 healthconnect-provider
   ```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_HOST` | PostgreSQL host | localhost |
| `POSTGRES_DB` | Database name | healthConnect |
| `POSTGRES_USER` | Database username | postgres |
| `POSTGRES_PASS` | Database password | postgres |
| `REDIS_HOST` | Redis host | localhost |
| `REDIS_PORT` | Redis port | 6379 |

### Application Profiles

- **dev** - Development environment
- **prod** - Production environment

Set the active profile using:
```bash
java -jar app.jar --spring.profiles.active=prod
```

## API Documentation

Once the application is running, access the API documentation at:
- **Swagger UI**: http://localhost:3012/swagger-ui.html
- **OpenAPI JSON**: http://localhost:3012/v3/api-docs

### Authentication

The API uses JWT-based authentication. Obtain a token by calling:

```bash
POST /api/v1/healthConnect/users/signin
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password"
}
```

Include the token in subsequent requests:
```bash
Authorization: Bearer <your-jwt-token>
```

### API Key Authentication

Some integration endpoints require API key authentication:
```bash
X-API-Key: hc_7f9a3b2e4d5c1f8e6a0d9b7c5e3f1a2d
```

## Key API Endpoints

### Eligibility Verification
```bash
GET /api/v1/healthConnect/eligibility/check/{providerUuid}
```

### Pharmacy Integration
```bash
POST /api/v1/healthConnect/integration/pharmacy/dispensing
GET /api/v1/healthConnect/integration/pharmacy/dispensing/{providerUuid}
```

### Claims Management
```bash
GET /api/v1/healthConnect/claims/allClaims
POST /api/v1/healthConnect/claims/{claimUuid}/approve
```

### Provider Management
```bash
GET /api/v1/healthConnect/provider/list
POST /api/v1/healthConnect/provider/create
```

## Integration Guide

For detailed integration instructions, especially for pharmacy systems, see:
- [Kenema Pharmacy Integration Guide](docs/kenema-pharmacy-integration.md)

## Development

### Project Structure

```
src/
├── main/
│   ├── java/com/medco/HealthConnectProvider/
│   │   ├── controller/          # REST controllers
│   │   ├── service/             # Business logic
│   │   ├── repository/          # Data access layer
│   │   ├── entity/              # JPA entities
│   │   ├── dto/                 # Data transfer objects
│   │   ├── config/              # Configuration classes
│   │   ├── security/            # Security configuration
│   │   └── utils/               # Utility classes
│   └── resources/
│       ├── application.properties
│       ├── db/changelog/        # Liquibase migrations
│       └── static/              # Static resources
└── test/                        # Test classes
```

### Running Tests

```bash
mvn test
```

### Code Style

The project uses standard Java conventions with Lombok for reducing boilerplate code.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is proprietary software developed by Medco Technology Solutions.

## Support

For support and questions, please contact the development team or create an issue in the project repository.

## Changelog

### Version 0.0.1-SNAPSHOT
- Initial release
- Basic provider and payer management
- Pharmacy integration capabilities
- Claims processing system
- User authentication and authorization
