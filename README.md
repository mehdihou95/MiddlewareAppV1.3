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
- Asynchronous file processing
- Automatic retry mechanism for failed processing
- Audit logging system
- Caching for improved performance
- Server-side pagination and sorting
- Client-specific data isolation
- Comprehensive security features

## Prerequisites

- Java 17 or higher
- Node.js 16 or higher
- Maven 3.6 or higher
- Git
- PostgreSQL 13+ (for production)

## Project Structure

```
.
├── backend/                # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── xml/
│   │   │   │           └── processor/
│   │   │   │               ├── annotation/  # Custom annotations
│   │   │   │               ├── aspect/      # AOP aspects
│   │   │   │               ├── config/      # Configuration classes
│   │   │   │               ├── controller/  # REST controllers
│   │   │   │               ├── converter/   # Data converters
│   │   │   │               ├── dto/         # Data Transfer Objects
│   │   │   │               ├── exception/   # Custom exceptions
│   │   │   │               ├── filter/      # Security filters
│   │   │   │               ├── mapper/      # Data mappers
│   │   │   │               ├── model/       # Domain models
│   │   │   │               ├── repository/  # Data access layer
│   │   │   │               ├── security/    # Security configuration
│   │   │   │               ├── service/     # Business logic
│   │   │   │               └── validation/  # Validation logic
│   │   │   └── resources/     # Configuration and migrations
│   │   └── test/             # Test files
│   └── pom.xml               # Maven configuration
├── frontend/                # React TypeScript frontend
│   ├── src/
│   │   ├── components/     # React components
│   │   ├── config/        # Configuration files
│   │   ├── context/       # React context providers
│   │   ├── pages/         # Page components
│   │   ├── services/      # API services
│   │   ├── types/         # TypeScript types
│   │   ├── utils/         # Utility functions
│   │   ├── App.tsx        # Main application component
│   │   └── index.tsx      # Application entry point
│   ├── public/            # Static files
│   ├── package.json       # npm configuration
│   └── tsconfig.json      # TypeScript configuration
├── docs/                  # Documentation files
└── Input/                # Sample XML input files
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

### Mapping Rules
- `GET /api/mapping-rules` - List mapping rules
- `POST /api/mapping-rules` - Create mapping rule
- `GET /api/mapping-rules/{id}` - Get rule details
- `PUT /api/mapping-rules/{id}` - Update rule
- `DELETE /api/mapping-rules/{id}` - Delete rule

### File Processing
- `POST /api/files/upload` - Upload XML file
- `GET /api/files/process/{id}` - Process file
- `GET /api/files/status/{id}` - Check processing status
- `POST /api/files/retry/{id}` - Retry failed processing

### Audit Logs
- `GET /api/audit-logs` - List audit logs
- `GET /api/audit-logs/{id}` - Get log details
- `GET /api/audit-logs/user/{username}` - Get user logs
- `GET /api/audit-logs/client/{clientId}` - Get client logs
- `GET /api/audit-logs/date-range` - Get logs by date range

## Configuration

### Backend Configuration
File: `backend/src/main/resources/application.properties`
```properties
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
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=500,expireAfterAccess=600s

# Logging Configuration
logging.level.root=INFO
logging.level.com.xml.processor=DEBUG
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
