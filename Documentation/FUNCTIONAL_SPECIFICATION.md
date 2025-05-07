# XML Middleware Application - Functional Specification

## 1. Overview
The XML Middleware Application is a multi-tenant system designed to process and transform XML documents according to client-specific rules. It provides a secure, scalable platform for handling XML data transformation with support for multiple clients and interfaces. The system supports two processing modes:
1. Synchronous Processing: Direct file uploads with immediate validation and response
2. Asynchronous Processing: RabbitMQ-based queue processing with retry mechanisms

## 2. User Roles and Access Control
### 2.1 Admin Users (ROLE_ADMIN)
- Full system access
- Client management capabilities (Create, Read, Update, Delete)
- Interface configuration
- Mapping rule management
- User management
- Audit log access
- System monitoring
- SFTP configuration management
- Queue management
- Cache management

### 2.2 Regular Users (ROLE_USER)
- Read-only access to client data
- View client list and details
- View interfaces
- View mapping rules
- View audit logs for assigned clients
- File upload capabilities
- View processing status
- Access monitoring dashboards

## 3. Core Features

### 3.1 User Authentication and Authorization
- JWT-based authentication with separate access and refresh tokens
- Access token lifetime: 1 hour
- Refresh token lifetime: 24 hours
- Role-based access control (ADMIN, USER)
- Multi-tenant data isolation using client context
- Automatic token refresh mechanism
- Session invalidation on logout
- Password encryption with BCrypt
- Failed login attempt tracking
- CSRF protection for non-GET requests
- Token blacklisting on logout
- Debug logging for authentication process
- Client context management in requests

### 3.2 Client Management
- Create and manage client profiles
- Client status tracking (active/inactive)
- Client code validation (regex: ^[A-Z0-9-_]+$)
- Client name uniqueness enforcement
- Client-specific settings
- Role-based access to client operations:
  * ADMIN: Full CRUD operations
  * USER: Read-only access
- Client context isolation
- Client selection in UI
- Client data persistence in H2 database
- Client-specific monitoring
- Client-specific caching rules

### 3.3 Security Features
- HTTPS encryption for all communications
- CSRF token protection
- JWT token validation and refresh
- Role-based endpoint security
- Client context isolation
- Token blacklisting
- Secure password storage
- Rate limiting for login attempts
- Debug logging for security operations
- Session management
- SFTP security configuration
- RabbitMQ security configuration
- Redis security configuration

### 3.4 Frontend Features
- Modern Material-UI based interface
- Responsive design
- Client management dashboard
- User authentication flows
- Token management
- CSRF token handling
- Error handling and display
- Loading states
- Navigation guards
- Role-based UI elements
- File upload interface
- Processing status tracking
- SFTP configuration interface
- Monitoring dashboards
- Cache management interface

### 3.5 API Security
- JWT authentication required for all non-auth endpoints
- CSRF token required for state-changing operations
- Role-based endpoint access control
- Client context validation
- Rate limiting
- Error handling with appropriate status codes
- Secure header management
- Token refresh mechanism
- Debug logging for API operations
- SFTP endpoint security
- RabbitMQ endpoint security
- Redis endpoint security

### 3.6 XML Processing
- Upload and validate XML files
- Dynamic mapping rule application
- Support for ASN document processing
- Real-time validation feedback
- Client-specific interface handling
- XML path validation
- Transformation rules application
- Header and line item processing
- Error handling and reporting
- Processing status tracking
- Automated SFTP processing
- RabbitMQ message processing
- Two processing modes:
  * Synchronous: Direct upload with immediate response
  * Asynchronous: Queue-based processing with retry
- Advanced features:
  * Smart batching with dynamic sizing
  * Dead Letter Queue (DLQ) handling
  * Schema version management
  * Validation result caching
  * File storage and metadata tracking

### 3.7 Mapping Rules
- Dynamic rule loading by client and interface
- XML path validation
- Support for multiple table types (ASN_HEADERS, ASN_LINES)
- Transformation rule application
- Default value handling
- Required field validation
- Priority-based processing
- Client-specific rule sets
- Interface-specific configurations
- Rule versioning
- Rule caching

### 3.8 Interface Management
- Configure input/output interfaces
- Define interface schemas
- Manage interface versions
- Monitor interface performance
- Interface-specific validation
- SFTP interface configuration
- RabbitMQ interface configuration
- Interface-specific caching rules
- Interface monitoring

### 3.9 Audit Logging
- Comprehensive activity tracking
- User action logging
- System event logging
- Performance monitoring
- Error tracking
- Security event logging
- File processing logs
- SFTP operation logs
- RabbitMQ message logs
- Cache operation logs
- Monitoring metrics

## 4. User Interface Requirements

### 4.1 Dashboard
- Processing statistics
- Recent activity
- Error reports
- Performance metrics
- Audit log viewer
- Client-specific views
- SFTP status monitoring
- Processing queue status
- Cache statistics
- System health metrics
- Batch processing metrics
- Queue depth monitoring

### 4.2 File Upload Interface
- Drag-and-drop functionality
- Multi-file upload
- Progress indicators
- Validation feedback
- Error reporting
- Processing status updates
- File history tracking
- Processing mode selection
- Batch size configuration
- Queue depth display

### 4.3 Rule Management Interface
- Rule editor
- Rule testing tools
- Version history
- Documentation tools
- Validation feedback
- Client-specific rules
- Rule caching status
- Rule performance metrics

### 4.4 Reporting Interface
- Processing history
- Error logs
- Performance reports
- Audit trails
- Client-specific reports
- Export capabilities
- SFTP operation reports
- Message queue reports
- Cache performance reports
- System health reports
- Two processing modes:
  * Synchronous: Real-time processing reports
  * Asynchronous: Queue-based processing reports

### 4.5 Audit Log Interface
- Filterable log viewer
- Search capabilities
- Date range selection
- User-specific views
- Client-specific views
- Export functionality
- SFTP operation logs
- Message queue logs
- Cache operation logs
- System event logs

## 5. Business Rules

### 5.1 Data Processing
- All XML files must be validated against schemas
- Processing must follow client-specific rules
- Failed validations must be logged
- Processed files must be archived
- Two processing modes:
  * Synchronous: Direct upload with immediate response
  * Asynchronous: Queue-based processing with retry
- Advanced features:
  * Smart batching with dynamic sizing
  * Dead Letter Queue (DLQ) handling
  * Schema version management
  * Validation result caching
  * File storage and metadata tracking
- SFTP file monitoring
- RabbitMQ message processing
- Cache management

### 5.2 Security
- Complete data isolation between clients using ClientContextHolder
- Encrypted data transmission over HTTPS
- Comprehensive audit logging of all user actions
- Regular security assessments and monitoring
- JWT-based authentication with token refresh
- Role-based access control (ADMIN, USER)
- Client-specific data isolation at service layer
- Password security with BCrypt encryption
- Session management with token invalidation
- Rate limiting for API endpoints
- SFTP security configuration
- RabbitMQ security configuration
- Redis security configuration

### 5.3 Performance
- Process files within 30 seconds (synchronous mode)
- Support concurrent processing
- Handle files up to 100MB
- Maintain 99.9% uptime
- Two processing modes:
  * Synchronous: Direct upload with immediate response
  * Asynchronous: Queue-based processing with retry
- Advanced features:
  * Smart batching with dynamic sizing
  * Dead Letter Queue (DLQ) handling
  * Schema version management
  * Validation result caching
  * File storage and metadata tracking
- SFTP polling interval: 60 seconds
- RabbitMQ message processing optimization
- Redis cache optimization

## 6. Compliance Requirements
- GDPR compliance for EU data
- Data retention policies
- Audit trail maintenance
- Security standards compliance
- Comprehensive logging
- Data isolation
- Access control
- SFTP security compliance
- Message queue security compliance
- Cache security compliance

## 7. Error Handling
- Clear error messages
- Retry mechanisms
- Error notification system
- Error recovery procedures
- Validation error handling
- Processing error handling
- System error handling
- SFTP error handling
- RabbitMQ error handling
- Cache error handling
- Two processing modes:
  * Synchronous: Immediate error response
  * Asynchronous: Queue-based error handling

## 8. Reporting Requirements
- Processing statistics
- Error reports
- Performance metrics
- Usage analytics
- Audit logs
- Client-specific reports
- System health reports
- SFTP operation reports
- Message queue reports
- Cache performance reports
- Two processing modes:
  * Synchronous: Real-time processing reports
  * Asynchronous: Queue-based processing reports

## 9. Integration Requirements
- REST API support
- Batch processing capabilities
- External system notifications
- Database integration
- File system integration
- Authentication service integration
- Audit logging integration
- SFTP integration
- RabbitMQ integration
- Redis integration
- Two processing modes:
  * Synchronous: Direct API integration
  * Asynchronous: Queue-based integration

## 10. Service Level Agreements
- 99.9% system availability
- Maximum 30-second processing time (synchronous mode)
- 24/7 system monitoring
- Regular backup procedures
- Comprehensive audit logging
- Error recovery procedures
- Performance monitoring
- SFTP monitoring
- Message queue monitoring
- Cache monitoring
- Two processing modes:
  * Synchronous: Immediate response SLA
  * Asynchronous: Queue-based processing SLA

## 11. Technical Implementation Details

### 11.1 Authentication Flow
1. User submits login credentials
2. Backend validates credentials and generates tokens
3. Frontend stores tokens securely
4. Access token used for subsequent requests
5. Refresh token used to obtain new access token
6. CSRF token included in state-changing requests
7. Tokens cleared on logout

### 11.2 Client Management Flow
1. Admin creates new client with unique name and code
2. Client status set to active/inactive
3. Client data isolated by context
4. Users assigned appropriate roles for client access
5. Client operations logged for audit purposes

### 11.3 Security Measures
1. BCrypt password encryption
2. JWT token validation
3. CSRF protection
4. Role-based access control
5. Client context isolation
6. Rate limiting
7. Token blacklisting
8. Secure session management
9. SFTP security configuration
10. RabbitMQ security configuration
11. Redis security configuration

## 12. Current Implementation Status

### 12.1 Completed Features
- Basic project structure setup
- Authentication system framework
- Client management framework
- Role-based access control
- Security configuration
- Frontend project setup
- Basic UI components
- API service structure
- ASN document processing
- Dynamic mapping rules
- XML path validation
- Header and line processing
- Client-specific interfaces
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
- Two processing modes:
  * Synchronous: Direct upload with immediate response
  * Asynchronous: Queue-based processing with retry
- Advanced features:
  * Smart batching with dynamic sizing
  * Dead Letter Queue (DLQ) handling
  * Schema version management
  * Validation result caching
  * File storage and metadata tracking
  * Circuit breaker pattern
  * Comprehensive monitoring

### 12.2 In Progress
- Additional document type support
- Enhanced validation rules
- Performance optimization
- Error handling improvements
- Testing coverage
- Documentation updates
- Cache optimization
- Queue management
- Monitoring enhancements

### 12.3 Known Issues
1. XML path validation edge cases
2. Mapping rule priority handling
3. Error message standardization
4. Performance optimization needed
5. Documentation updates required
6. Cache invalidation timing
7. Queue depth management
8. Resource utilization

### 12.4 Next Steps
1. Add support for additional document types
2. Enhance validation rules
3. Optimize performance
4. Improve error handling
5. Add comprehensive testing
6. Update documentation
7. Optimize caching
8. Enhance monitoring
9. Improve queue management
10. Implement advanced features 