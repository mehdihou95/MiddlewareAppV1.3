# Backend Architecture Documentation

## 1. System Overview

### 1.1 Core Components
```mermaid
graph TB
    Client[Frontend Client]
    Auth[Authentication Layer]
    Security[Security Filters]
    Controllers[REST Controllers]
    Services[Service Layer]
    Strategy[Document Processing Strategy]
    Mapping[Mapping Rules]
    Repos[Repository Layer]
    DB[(H2 Database)]
    RMQ[RabbitMQ]
    SFTP[SFTP Server]

    Client --> Auth
    Auth --> Security
    Security --> Controllers
    Controllers --> Services
    Services --> Strategy
    Strategy --> Mapping
    Strategy --> Repos
    Repos --> DB
    SFTP --> RMQ
    RMQ --> Services
```

## 2. Project Structure

### 2.1 Package Organization
```
com.xml.processor/
├── annotation/      # Custom annotations
├── aspect/         # AOP aspects
├── config/         # Configuration classes
├── controller/     # REST controllers
├── converter/      # Data converters
├── dto/           # Data Transfer Objects
├── exception/     # Custom exceptions
├── filter/        # Security filters
├── mapper/        # Data mappers
├── model/         # Domain models
├── repository/    # Data access layer
├── security/      # Security configuration
├── service/       # Business logic
│   └── strategy/  # Document processing strategies
├── validation/    # Validation logic
└── listener/      # Message listeners
    └── connectors/
        └── sftp/  # SFTP integration
```

## 3. Component Responsibilities

### 3.1 Security Layer
- **JwtAuthenticationFilter**
  - Location: `security/filter/JwtAuthenticationFilter.java`
  - Responsibilities:
    1. Extract JWT from request
    2. Validate token
    3. Load user details
    4. Set authentication context

- **ClientContextFilter**
  - Location: `filter/ClientContextFilter.java`
  - Responsibilities:
    1. Extract client context from request
    2. Validate client access
    3. Set client context holder

### 3.2 Document Processing Strategies
- **AsnDocumentProcessingStrategy**
  - Location: `service/strategy/AsnDocumentProcessingStrategy.java`
  - Responsibilities:
    1. Process ASN XML documents
    2. Apply mapping rules dynamically
    3. Transform data according to rules
    4. Handle header and line items
    5. Validate XML paths
    6. Store processed data

### 3.3 Mapping Rules
- **MappingRuleService**
  - Location: `service/impl/MappingRuleServiceImpl.java`
  - Responsibilities:
    1. Load client-specific mapping rules
    2. Validate XML paths
    3. Apply transformations
    4. Handle different table types (ASN_HEADERS, ASN_LINES)

### 3.4 Controllers Layer
- **AuthController**
  - Location: `controller/AuthController.java`
  - Endpoints:
    * POST `/api/auth/login`
    * POST `/api/auth/logout`
    * POST `/api/auth/refresh`
    * GET `/api/auth/validate`

- **ClientController**
  - Location: `controller/ClientController.java`
  - Endpoints:
    * GET `/api/clients`
    * POST `/api/clients`
    * GET `/api/clients/{id}`
    * PUT `/api/clients/{id}`
    * DELETE `/api/clients/{id}`

- **FileUploadController**
  - Location: `controller/FileUploadController.java`
  - Endpoints:
    * POST `/api/upload`
    * GET `/api/upload/status/{id}`

- **SftpConfigController**
  - Location: `listener/connectors/sftp/controller/SftpConfigController.java`
  - Endpoints:
    * POST `/api/sftp/config`
    * GET `/api/sftp/config`
    * PUT `/api/sftp/config/{id}`
    * DELETE `/api/sftp/config/{id}`

### 3.5 Service Layer
- **JwtService**
  - Location: `security/service/JwtService.java`
  - Responsibilities:
    1. Generate access tokens
    2. Generate refresh tokens
    3. Validate tokens
    4. Extract claims
    5. Handle token blacklisting

- **UserService**
  - Location: `service/impl/UserServiceImpl.java`
  - Responsibilities:
    1. User CRUD operations
    2. Password management
    3. Role management
    4. User authentication

- **ClientService**
  - Location: `service/impl/ClientServiceImpl.java`
  - Responsibilities:
    1. Client CRUD operations
    2. Client validation
    3. Client context management

- **XmlProcessorService**
  - Location: `service/impl/XmlProcessorServiceImpl.java`
  - Responsibilities:
    1. Process XML files
    2. Apply document strategies
    3. Handle file validation
    4. Manage processing status

- **SftpConfigService**
  - Location: `listener/connectors/sftp/service/SftpConfigService.java`
  - Responsibilities:
    1. Manage SFTP configurations
    2. Handle route creation
    3. Monitor file changes
    4. Process incoming files

## 4. Process Flows

### 4.1 Frontend Upload Flow
```mermaid
sequenceDiagram
    participant FE as Frontend
    participant FC as FileUploadController
    participant XPS as XmlProcessorServiceImpl
    participant ADS as AsnDocumentProcessingStrategy
    participant MRS as MappingRuleServiceImpl
    participant ASNS as AsnServiceImpl
    participant PFS as ProcessedFileServiceImpl
    participant DB as Database

    FE->>FC: POST /api/upload (MultipartFile)
    Note over FC: Validate file and headers
    
    FC->>PFS: createProcessedFile(status=PROCESSING)
    PFS->>DB: Insert into processed_files
    
    FC->>XPS: processXmlFile()
    XPS->>ADS: processDocument()
    
    ADS->>MRS: Get mapping rules for ASN_HEADERS
    MRS->>DB: Query mapping_rules table
    DB-->>MRS: Return header rules
    
    ADS->>MRS: Get mapping rules for ASN_LINES
    MRS->>DB: Query mapping_rules table
    DB-->>MRS: Return line rules
    
    Note over ADS: Create AsnHeader object
    Note over ADS: Apply header mapping rules
    
    ADS->>ASNS: createAsnHeader()
    ASNS->>DB: Insert into asn_headers
    
    Note over ADS: Create AsnLine objects
    Note over ADS: Apply line mapping rules
    
    ADS->>ASNS: createAsnLines()
    ASNS->>DB: Insert into asn_lines
    
    ADS->>PFS: Update processed file status
    PFS->>DB: Update processed_files status=SUCCESS
    
    FC-->>FE: Return processing result
```

### 4.2 Message Processor Flow
```mermaid
sequenceDiagram
    participant RMQ as RabbitMQ
    participant IML as InboundMessageListener
    participant XPS as XmlProcessorServiceImpl
    participant ADS as AsnDocumentProcessingStrategy
    participant MRS as MappingRuleServiceImpl
    participant ASNS as AsnServiceImpl
    participant PFS as ProcessedFileServiceImpl
    participant DB as Database

    RMQ->>IML: Send message with XML file
    Note over IML: @RabbitListener processes message
    
    IML->>PFS: createProcessedFile(status=PROCESSING)
    PFS->>DB: Insert into processed_files
    
    IML->>XPS: processXmlFile()
    XPS->>ADS: processDocument()
    
    ADS->>MRS: Get mapping rules for ASN_HEADERS
    MRS->>DB: Query mapping_rules table
    DB-->>MRS: Return header rules
    
    ADS->>MRS: Get mapping rules for ASN_LINES
    MRS->>DB: Query mapping_rules table
    DB-->>MRS: Return line rules
    
    Note over ADS: Create AsnHeader object
    Note over ADS: Apply header mapping rules
    
    ADS->>ASNS: createAsnHeader()
    ASNS->>DB: Insert into asn_headers
    
    Note over ADS: Create AsnLine objects
    Note over ADS: Apply line mapping rules
    
    ADS->>ASNS: createAsnLines()
    ASNS->>DB: Insert into asn_lines
    
    ADS->>PFS: Update processed file status
    PFS->>DB: Update processed_files status=SUCCESS
    
    IML-->>RMQ: Acknowledge message
```

### 4.3 SFTP Integration Flow
```mermaid
sequenceDiagram
    participant FE as Frontend
    participant SC as SftpConfigController
    participant SS as SftpConfigService
    participant SCC as SftpCamelConfig
    participant SFTP as SFTP Server
    participant RMQ as RabbitMQ
    participant PR as InboundMessageListener

    FE->>SC: POST /api/sftp/config
    Note over SC: Receives SFTP configuration
    SC->>SS: saveConfiguration()
    Note over SS: Stores config in database
    
    SS-->>SCC: @Scheduled getActiveConfigurations()
    Note over SCC: Polls every 60s for config changes
    
    SCC->>SCC: createOrUpdateRoute()
    Note over SCC: Creates SFTP routes for monitoring
    
    SFTP-->>SCC: New file detected
    Note over SCC: File processing starts
    
    SCC->>RMQ: Send to middleware.direct exchange
    Note over SCC: with inbound.processor routing key
    
    RMQ->>PR: Push message to processor
    Note over PR: @RabbitListener processes message
    
    PR-->>RMQ: Acknowledge message
    
    SCC->>SFTP: Move file to ok/ko directory
    Note over SCC: Based on processing result
```

### 4.4 Service Interaction Flow
```mermaid
sequenceDiagram
    participant XPI as XmlProcessorService
    participant XPSI as XmlProcessorServiceImpl
    participant ADS as AsnDocumentProcessingStrategy
    participant MRI as MappingRuleService
    participant MRSI as MappingRuleServiceImpl
    participant ASI as AsnService
    participant ASSI as AsnServiceImpl
    participant PFI as ProcessedFileService
    participant PFSI as ProcessedFileServiceImpl

    Note over XPI,PFSI: Service Dependencies and Flow
    
    Note over XPSI: @Service implements XmlProcessorService
    Note over MRSI: @Service implements MappingRuleService
    Note over ASSI: @Service implements AsnService
    Note over PFSI: @Service implements ProcessedFileService
    
    XPSI->>ADS: Uses strategy pattern for document processing
    
    ADS->>MRI: findByTableNameAndClient_Id()
    MRI->>MRSI: Delegates to implementation
    MRSI-->>ADS: Returns mapping rules
    
    ADS->>ASI: createAsnHeader()
    ASI->>ASSI: Delegates to implementation
    ASSI-->>ADS: Returns created header
    
    ADS->>ASI: createAsnLines()
    ASI->>ASSI: Delegates to implementation
    ASSI-->>ADS: Returns created lines
    
    ADS->>PFI: findMostRecentByFileNameAndInterfaceId()
    PFI->>PFSI: Delegates to implementation
    PFSI-->>ADS: Returns processed file
    
    ADS->>PFI: createProcessedFile()
    PFI->>PFSI: Delegates to implementation
    PFSI-->>ADS: Returns created record
```

## 5. Security Configuration

### 5.1 Filter Chain Order
1. CorsFilter (Order: -100)
2. ClientContextFilter (Order: 1)
3. JwtAuthenticationFilter (Order: 2)
4. CsrfFilter (Order: 3)

### 5.2 Security Rules
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/clients/**").hasAnyRole("ADMIN", "USER")
    .requestMatchers(HttpMethod.POST, "/api/clients/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.PUT, "/api/clients/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.DELETE, "/api/clients/**").hasRole("ADMIN")
    .anyRequest().authenticated()
)
```

## 6. Current Implementation Status

### 6.1 Completed Features
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

### 6.2 In Progress
- Additional document type support
- Enhanced validation rules
- Performance optimization
- Error handling improvements
- Testing coverage
- Documentation updates

### 6.3 Known Issues
1. XML path validation edge cases
2. Mapping rule priority handling
3. Error message standardization
4. Performance optimization needed
5. Documentation updates required

### 6.4 Next Steps
1. Add support for additional document types
2. Enhance validation rules
3. Optimize performance
4. Improve error handling
5. Add comprehensive testing
6. Update documentation 