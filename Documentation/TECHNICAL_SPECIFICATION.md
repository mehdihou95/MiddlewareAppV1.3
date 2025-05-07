# XML Middleware Application - Technical Specification

## 1. System Architecture

### 1.1 Overview
The application follows a microservices architecture with a clear separation between frontend and backend components. It uses Spring Boot for the backend and React with TypeScript for the frontend, communicating via RESTful APIs. The system integrates with SFTP servers and RabbitMQ for automated file processing.

### 1.2 Components
- Frontend (React with TypeScript)
  * Material-UI components
  * JWT authentication
  * CSRF protection
  * Client context management
  * Role-based UI
  * File upload interface
  * Processing status tracking
- Backend (Spring Boot)
  * JWT authentication service
  * Client management service
  * Role-based security
  * H2 database integration
  * SFTP integration
  * RabbitMQ integration
- Database (H2)
  * User management
  * Client management
  * Role management
  * Audit logging
  * File processing status
- Message Queue (RabbitMQ)
  * Message routing
  * Processing queue
  * Error handling
- SFTP Server
  * File monitoring
  * Secure file transfer
  * Directory management

## 2. Technology Stack

### 2.1 Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 17
- **Build Tool**: Maven
- **Database**: H2 (Development)
- **Security**: 
  * Spring Security with JWT
  * CSRF protection
  * Role-based access control
  * Client context isolation
  * SFTP security
  * RabbitMQ security
- **Testing**: JUnit, Mockito
- **Logging**: SLF4J with Logback
- **Integration**:
  * Apache Camel for SFTP
  * Spring AMQP for RabbitMQ
  * Spring Integration

### 2.2 Frontend
- **Framework**: React 18.x
- **Language**: TypeScript
- **State Management**: Context API
- **UI Components**: Material-UI
- **HTTP Client**: Axios with interceptors
- **Authentication**: JWT with refresh tokens
- **Security**: CSRF protection
- **Build Tool**: npm
- **Error Handling**: Custom error handler with logging
- **File Upload**: React Dropzone
- **Status Tracking**: WebSocket integration

## 3. Project Structure

### 3.1 Backend Structure
```
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── xml/
│   │   │           └── processor/
│   │   │               ├── annotation/
│   │   │               ├── aspect/
│   │   │               ├── config/
│   │   │               ├── controller/
│   │   │               ├── converter/
│   │   │               ├── dto/
│   │   │               ├── exception/
│   │   │               ├── filter/
│   │   │               ├── mapper/
│   │   │               ├── model/
│   │   │               ├── repository/
│   │   │               ├── security/
│   │   │               ├── service/
│   │   │               ├── validation/
│   │   │               └── listener/
│   │   │                   └── connectors/
│   │   │                       └── sftp/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
└── pom.xml
```

### 3.2 Frontend Structure
```
frontend/
├── src/
│   ├── components/
│   ├── config/
│   ├── context/
│   ├── pages/
│   ├── services/
│   ├── types/
│   ├── utils/
│   ├── App.tsx
│   └── index.tsx
├── public/
├── package.json
└── tsconfig.json
```

## 4. API Endpoints

### 4.1 Authentication
```
POST /api/auth/login
Request: {
    "username": string,
    "password": string
}
Response: {
    "token": string,
    "refreshToken": string,
    "username": string,
    "roles": string[],
    "csrfToken": string
}

POST /api/auth/logout
Request: Headers: {
    "Authorization": "Bearer {token}"
}
Response: {
    "message": "Logged out successfully"
}

POST /api/auth/refresh
Request: {
    "refreshToken": string
}
Response: {
    "token": string,
    "refreshToken": string,
    "username": string,
    "roles": string[]
}

GET /api/auth/validate
Response: {
    "valid": boolean,
    "username": string,
    "roles": string[]
}
```

### 4.2 Client Management
```
GET /api/clients
Headers: {
    "Authorization": "Bearer {token}",
    "X-XSRF-TOKEN": "{csrfToken}"
}
Response: {
    "content": Client[],
    "totalElements": number,
    "totalPages": number,
    "size": number,
    "number": number
}

POST /api/clients
Headers: {
    "Authorization": "Bearer {token}",
    "X-XSRF-TOKEN": "{csrfToken}"
}
Request: {
    "name": string,
    "code": string,
    "active": boolean
}
Response: Client

GET /api/clients/{id}
Headers: {
    "Authorization": "Bearer {token}"
}
Response: Client

PUT /api/clients/{id}
Headers: {
    "Authorization": "Bearer {token}",
    "X-XSRF-TOKEN": "{csrfToken}"
}
Request: {
    "name": string,
    "code": string,
    "active": boolean
}
Response: Client

DELETE /api/clients/{id}
Headers: {
    "Authorization": "Bearer {token}",
    "X-XSRF-TOKEN": "{csrfToken}"
}
```

### 4.3 File Processing
```
POST /api/upload
Headers: {
    "Authorization": "Bearer {token}",
    "X-XSRF-TOKEN": "{csrfToken}"
}
Request: MultipartFile
Response: {
    "id": string,
    "status": string,
    "message": string
}

GET /api/upload/status/{id}
Headers: {
    "Authorization": "Bearer {token}"
}
Response: {
    "id": string,
    "status": string,
    "message": string,
    "processedAt": string
}
```

### 4.4 SFTP Configuration
```
POST /api/sftp/config
Headers: {
    "Authorization": "Bearer {token}",
    "X-XSRF-TOKEN": "{csrfToken}"
}
Request: {
    "host": string,
    "port": number,
    "username": string,
    "password": string,
    "directory": string,
    "clientId": string
}
Response: SftpConfig

GET /api/sftp/config
Headers: {
    "Authorization": "Bearer {token}"
}
Response: SftpConfig[]

PUT /api/sftp/config/{id}
Headers: {
    "Authorization": "Bearer {token}",
    "X-XSRF-TOKEN": "{csrfToken}"
}
Request: {
    "host": string,
    "port": number,
    "username": string,
    "password": string,
    "directory": string,
    "clientId": string
}
Response: SftpConfig

DELETE /api/sftp/config/{id}
Headers: {
    "Authorization": "Bearer {token}",
    "X-XSRF-TOKEN": "{csrfToken}"
}
```

## 5. Security Implementation

### 5.1 Authentication Flow
1. User submits credentials
2. Backend validates credentials
3. JWT tokens generated (access + refresh)
4. CSRF token generated
5. Tokens returned to frontend
6. Frontend stores tokens
7. Tokens included in subsequent requests
8. Auto refresh on token expiration

### 5.2 JWT Configuration
```java
@Configuration
public class JwtConfig {
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;
    
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration; // 1 hour
    
    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration; // 24 hours
}
```

### 5.3 Security Filter Chain
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/clients/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.POST, "/api/clients/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/clients/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/clients/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(requestHandler)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

## 6. XML Processing Implementation

### 6.1 Document Processing Strategy
```typescript
@Component
public class AsnDocumentProcessingStrategy extends AbstractDocumentProcessingStrategy {
    @Override
    public ProcessedFile processDocument(MultipartFile file, Interface interfaceEntity) {
        // Load mapping rules
        List<MappingRule> headerRules = mappingRuleRepository.findByTableNameAndClient_Id(
            "ASN_HEADERS", 
            interfaceEntity.getClient().getId()
        );
        
        List<MappingRule> lineRules = mappingRuleRepository.findByTableNameAndClient_Id(
            "ASN_LINES", 
            interfaceEntity.getClient().getId()
        );
        
        // Process document
        Document document = parseXmlFile(file);
        
        // Apply mapping rules
        AsnHeader header = processHeader(document, headerRules);
        List<AsnLine> lines = processLines(document, lineRules);
        
        // Store results
        return saveProcessedData(header, lines);
    }
}
```

### 6.2 Mapping Rule Application
```typescript
private void setFieldValue(Object entity, MappingRule rule, String value) {
    // Get field name and create setter name
    String fieldName = rule.getDatabaseField();
    String setterName = "set" + fieldName.substring(0, 1).toUpperCase() 
        + fieldName.substring(1);
    
    // Apply transformations if needed
    if (rule.getTransformation() != null) {
        value = applyTransformation(value, rule.getTransformation());
    }
    
    // Set value using reflection
    Method setter = entity.getClass().getMethod(setterName, String.class);
    setter.invoke(entity, value);
}
```

### 6.3 XML Path Validation
```typescript
private String getNodeValue(XPath xPath, String xpath, Node contextNode) {
    try {
        Node node = (Node) xPath.evaluate(xpath, contextNode, XPathConstants.NODE);
        return node != null ? node.getTextContent() : null;
    } catch (Exception e) {
        logger.error("Error evaluating XPath: {}", xpath);
        throw new ValidationException("Invalid XML path: " + xpath);
    }
}
```

## 7. Current Implementation Status

### 7.1 Completed Features
- Project structure setup
- Authentication framework
- Role-based access control
- CSRF protection
- Client CRUD operations
- ASN document processing
- Dynamic mapping rules
- XML path validation
- Header and line processing
- Interface management
- Mapping rules system
- Enhanced error handling
- Improved logging
- Performance optimization
- Testing coverage
- Documentation updates
- SFTP integration
- RabbitMQ integration
- File processing status tracking
- Multi-tenant support
- Client context isolation

### 7.2 In Progress
- Additional document type support
- Enhanced validation rules
- Performance optimization
- Error handling improvements
- Testing coverage
- Documentation updates

### 7.3 Known Issues
1. XML path validation edge cases
2. Mapping rule priority handling
3. Error message standardization
4. Performance optimization needed
5. Documentation updates required

### 7.4 Next Steps
1. Add support for additional document types
2. Enhance validation rules
3. Optimize performance
4. Improve error handling
5. Add comprehensive testing
6. Update documentation 