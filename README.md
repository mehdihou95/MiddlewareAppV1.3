# XML Middleware Application

This application is a multi-tenant middleware solution for processing XML files using Spring Boot and React. It provides XML validation, transformation, and storage capabilities with a modern web interface and client-specific configurations.

## Features

- Multi-tenant XML file processing with XSD validation
- Client-specific mapping rules and configurations
- Role-based access control with multi-tenant security
- Real-time processing status and monitoring
- Modern React-based UI with Material-UI components
- H2 database for development, PostgreSQL for production
- Flyway database migrations
- JWT-based authentication with token refresh
- Client performance monitoring
- Comprehensive error handling and logging
- Two processing modes:
  * Asynchronous: RabbitMQ-based queue processing
  * Synchronous: Direct file upload processing
- Advanced features:
  * Smart batching with dynamic sizing
  * Dead Letter Queue (DLQ) and retry mechanisms
  * Schema version management
  * Validation result caching
  * File storage and metadata tracking
  * Circuit breaker pattern for resilience
  * Comprehensive monitoring and metrics

## Prerequisites

- Java 17 or higher
- Node.js 16 or higher
- Maven 3.6 or higher
- Git
- PostgreSQL 13+ (for production)
- Redis 6+ (for caching)
- RabbitMQ 3.8+ (for async processing)

## Project Structure

```
.
├── backend/                # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── middleware/
│   │   │   │           ├── processor/     # Processing module
│   │   │   │           │   ├── config/    # Configuration classes
│   │   │   │           │   ├── service/   # Business logic
│   │   │   │           │   ├── validation/# Validation logic
│   │   │   │           │   └── cache/     # Caching components
│   │   │   │           └── shared/        # Shared module
│   │   │   │               ├── model/     # Domain models
│   │   │   │               ├── repository/# Data access layer
│   │   │   │               └── config/    # Shared configuration
│   │   │   └── resources/  # Configuration and migrations
│   │   └── test/          # Test files
│   └── pom.xml            # Maven configuration
├── frontend/              # React TypeScript frontend
│   ├── src/
│   │   ├── components/   # React components
│   │   ├── config/      # Configuration files
│   │   ├── context/     # React context providers
│   │   ├── pages/       # Page components
│   │   ├── services/    # API services
│   │   ├── types/       # TypeScript types
│   │   ├── utils/       # Utility functions
│   │   ├── App.tsx      # Main application component
│   │   └── index.tsx    # Application entry point
│   ├── public/          # Static files
│   ├── package.json     # npm configuration
│   └── tsconfig.json    # TypeScript configuration
├── docs/                # Documentation files
└── Input/              # Sample XML input files
```

## Setup

1. Clone the repository:
```bash
git clone https://github.com/mehdihou95/MiddlewareAppV1.1.git
cd MiddlewareAppV1.1
```

2. Build and run the backend:
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

3. Set up and run the frontend:
```bash
cd frontend
npm install
npm start
```

## Accessing the Application

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console
  - JDBC URL: jdbc:h2:mem:testdb
  - Username: sa
  - Password: password
- Swagger UI: http://localhost:8080/swagger-ui.html
- RabbitMQ Management: http://localhost:15672
  - Username: guest
  - Password: guest
- Redis Commander: http://localhost:8081

## Default Users

The application comes with predefined users:
- Admin: username: `admin`, password: `admin`
  - Full system access
  - Client management
  - Interface configuration
  - User management
  - Audit log access
  - System monitoring
- Client User: username: `user`, password: `user`
  - Client-specific access
  - File processing
  - Mapping rule management
  - View client-specific audit logs

## Core API Endpoints

### Authentication
- `POST /api/auth/login` - User authentication
- `POST /api/auth/refresh` - Refresh JWT token
- `GET /api/auth/user` - Get current user

### Client Management
- `GET /api/clients` - List clients (paginated)
- `POST /api/clients` - Create client
- `GET /api/clients/{id}` - Get client details
- `PUT /api/clients/{id}` - Update client
- `DELETE /api/clients/{id}` - Delete client

### Interface Management
- `GET /api/interfaces` - List interfaces
- `POST /api/interfaces` - Create interface
- `GET /api/interfaces/{id}` - Get interface details
- `PUT /api/interfaces/{id}` - Update interface
- `DELETE /api/interfaces/{id}` - Delete interface

### File Processing
- `POST /api/files/upload` - Upload XML file (Synchronous)
- `POST /api/files/queue` - Queue XML file (Asynchronous)
- `GET /api/files/process/{id}` - Process file
- `GET /api/files/status/{id}` - Check processing status
- `POST /api/files/retry/{id}` - Retry failed processing
- `GET /api/files/metadata/{id}` - Get file metadata

### Monitoring
- `GET /api/monitoring/batch` - Get batch processing metrics
- `GET /api/monitoring/queue` - Get queue metrics
- `GET /api/monitoring/cache` - Get cache metrics
- `GET /api/monitoring/health` - Get system health status

## Configuration

### Backend Configuration
File: `backend/src/main/resources/application.yml`
```yaml
# Database Configuration
spring.datasource.url=jdbc:h2:file:./data/middleware
spring.datasource.username=sa
spring.datasource.password=password

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Server Configuration
server.port=8080

# JWT Configuration
jwt.secret=your_jwt_secret_key
jwt.expiration=3600000
jwt.refresh-token.expiration=86400000

# File Upload Configuration
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB

# Cache Configuration
spring.cache.type=redis
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=
spring.redis.database=0

# RabbitMQ Configuration
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Batch Processing Configuration
batch:
  core-pool-size: 5
  max-pool-size: 10
  queue-capacity: 25
  thread-name-prefix: BatchProcessor-
  size: 100
  timeout-seconds: 300
  min-size: 10
  max-size: 100
  queue-depth-threshold: 1000
  adjustment-step: 10

# Validation Configuration
xml:
  validation:
    entity-expansion-limit: 0
    honour-all-schema-locations: true
    enable-external-dtd: false
    enable-external-schema: false
    enable-schema-full-checking: false
    max-memory-size: 10485760
```

### Frontend Configuration
File: `frontend/.env`
```properties
REACT_APP_API_URL=http://localhost:8080/api
REACT_APP_JWT_EXPIRATION=3600000
REACT_APP_REFRESH_TOKEN_EXPIRATION=86400000
```

## Documentation

- [Backend Architecture](BACKEND_ARCHITECTURE.md)
- [Functional Specification](FUNCTIONAL_SPECIFICATION.md)
- [Technical Specification](TECHNICAL_SPECIFICATION.md)
- [API Documentation](http://localhost:8080/swagger-ui.html)

## Testing

1. Run backend tests:
```bash
cd backend
mvn test
```

2. Run frontend tests:
```bash
cd frontend
npm test
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Current Implementation Status

### Completed Features
- Project structure setup
- Basic authentication framework
- Role-based access control
- CSRF protection
- Client CRUD operations
- Token refresh mechanism
- Client context isolation
- Basic audit logging
- Error handling framework
- XML processing implementation
- Interface management
- Mapping rules system
- Enhanced error handling
- Improved logging
- Performance optimization
- Testing coverage
- Documentation updates
- Two processing modes:
  * Asynchronous (RabbitMQ)
  * Synchronous (Direct upload)
- Advanced features:
  * Smart batching
  * DLQ and retry mechanisms
  * Schema version management
  * Validation caching
  * File storage and metadata
  * Circuit breaker pattern
  * Comprehensive monitoring

### In Progress
- Additional document type support
- Enhanced validation rules
- Performance optimization
- Error handling improvements
- Testing coverage
- Documentation updates

### Known Issues
1. Role prefix handling in authentication
2. Token refresh edge cases
3. CSRF token refresh scenarios
4. Client context persistence
5. Error message standardization

### Next Steps
1. Add support for additional document types
2. Enhance validation rules
3. Optimize performance
4. Improve error handling
5. Add comprehensive testing
6. Complete documentation


Don't update --------------------------------------------
run in the background : cd backend; mvn spring-boot:run
run in the background : cd frontend; npm start

Create a new endpoint to fetch available strategy types dynamically

Add the file processing queue (code provided above)
Implement parallel processing with thread pools
Add basic monitoring
Implement a simple cleanup strategy for processed files

---------------------------------------------------------------------
