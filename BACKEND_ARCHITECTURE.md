# Backend Architecture

## Overview

The backend is built using Spring Boot and follows a modular architecture with clear separation of concerns. It consists of two main modules:

1. **Processor Module**: Handles XML processing, validation, and business logic
2. **Shared Module**: Contains shared models, repositories, and configurations

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Application                        │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                         API Gateway                              │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Security Layer                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  JWT Auth   │  │  CSRF       │  │  Client Context         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Processing Layer                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  Sync       │  │  Async      │  │  Batch Processing       │  │
│  │  Processing │  │  Processing │  │                         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Business Layer                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  Validation │  │  Mapping    │  │  Transformation         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Data Access Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  Repositories│  │  Cache      │  │  File Storage          │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Infrastructure Layer                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  Database   │  │  RabbitMQ   │  │  Redis                  │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Key Components

### 1. Security Layer

- **JWT Authentication**
  - Token-based authentication
  - Token refresh mechanism
  - Role-based access control
  - Client context isolation

- **CSRF Protection**
  - CSRF token generation
  - Token validation
  - Secure cookie handling

- **Client Context**
  - Tenant isolation
  - Client-specific configurations
  - Context propagation

### 2. Processing Layer

- **Synchronous Processing**
  - Direct file upload
  - Immediate validation
  - Real-time response
  - Transaction management

- **Asynchronous Processing**
  - RabbitMQ-based queue
  - Message persistence
  - Retry mechanisms
  - Dead Letter Queue (DLQ)

- **Batch Processing**
  - Dynamic batch sizing
  - Load-based adjustment
  - Queue depth monitoring
  - Performance optimization

### 3. Business Layer

- **Validation**
  - Schema version management
  - Content-based validation
  - Validation result caching
  - Error handling

- **Mapping**
  - Client-specific rules
  - Dynamic mapping
  - Rule versioning
  - Transformation logic

- **Transformation**
  - XML to entity mapping
  - Data enrichment
  - Format conversion
  - Error recovery

### 4. Data Access Layer

- **Repositories**
  - JPA-based data access
  - Query optimization
  - Transaction management
  - Audit logging

- **Cache**
  - Redis-based caching
  - TTL management
  - Cache invalidation
  - Performance monitoring

- **File Storage**
  - Metadata tracking
  - Version control
  - Access logging
  - Storage optimization

### 5. Infrastructure Layer

- **Database**
  - H2 (Development)
  - PostgreSQL (Production)
  - Flyway migrations
  - Connection pooling

- **Message Queue**
  - RabbitMQ
  - Queue management
  - Message persistence
  - Retry policies

- **Cache**
  - Redis
  - Cache management
  - Data persistence
  - Monitoring

## Processing Modes

### 1. Synchronous Processing

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │────▶│   API       │────▶│  Validation │
└─────────────┘     └─────────────┘     └─────────────┘
                                    │
                                    ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Response   │◀────│  Processing │◀────│  Mapping    │
└─────────────┘     └─────────────┘     └─────────────┘
```

### 2. Asynchronous Processing

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │────▶│   API       │────▶│  RabbitMQ   │
└─────────────┘     └─────────────┘     └─────────────┘
                                    │
                                    ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Status     │◀────│  Processor  │◀────│  DLQ        │
└─────────────┘     └─────────────┘     └─────────────┘
```

## Advanced Features

### 1. Smart Batching

- Dynamic batch size adjustment
- Queue depth monitoring
- System load consideration
- Performance optimization

### 2. Error Recovery

- Dead Letter Queue (DLQ)
- Retry mechanisms
- Exponential backoff
- Error tracking

### 3. Schema Management

- Version control
- Schema caching
- Validation optimization
- Error handling

### 4. Caching Strategy

- Redis-based caching
- TTL management
- Cache invalidation
- Performance monitoring

### 5. Monitoring

- Batch processing metrics
- Queue metrics
- Cache metrics
- System health

## Configuration

### 1. Application Configuration

```yaml
# Database
spring.datasource.url=jdbc:h2:file:./data/middleware
spring.datasource.username=sa
spring.datasource.password=password

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Server
server.port=8080

# JWT
jwt.secret=your_jwt_secret_key
jwt.expiration=3600000
jwt.refresh-token.expiration=86400000

# File Upload
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB

# Cache
spring.cache.type=redis
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=
spring.redis.database=0

# RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Batch Processing
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

# Validation
xml:
  validation:
    entity-expansion-limit: 0
    honour-all-schema-locations: true
    enable-external-dtd: false
    enable-external-schema: false
    enable-schema-full-checking: false
    max-memory-size: 10485760
```

### 2. Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .antMatchers("/api/auth/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}
```

### 3. RabbitMQ Configuration

```java
@Configuration
public class RabbitMQConfig {
    @Bean
    public Queue processingQueue() {
        return QueueBuilder.durable("processing.queue")
            .withArgument("x-dead-letter-exchange", "dlx.exchange")
            .withArgument("x-dead-letter-routing-key", "dlq.queue")
            .build();
    }

    @Bean
    public Exchange dlxExchange() {
        return ExchangeBuilder.directExchange("dlx.exchange").durable(true).build();
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable("dlq.queue")
            .withArgument("x-message-ttl", 86400000) // 24 hours
            .build();
    }
}
```

### 4. Redis Configuration

```java
@Configuration
@EnableCaching
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
```

## Current Implementation Status

### Completed Features

1. **Core Architecture**
   - Modular structure
   - Clear separation of concerns
   - Dependency management
   - Configuration management

2. **Security**
   - JWT authentication
   - CSRF protection
   - Role-based access
   - Client context

3. **Processing**
   - Synchronous mode
   - Asynchronous mode
   - Batch processing
   - Error handling

4. **Advanced Features**
   - Smart batching
   - DLQ and retry
   - Schema management
   - Caching
   - Monitoring

### In Progress

1. **Performance Optimization**
   - Query optimization
   - Cache tuning
   - Batch size adjustment
   - Resource management

2. **Error Handling**
   - Error recovery
   - Retry policies
   - Error tracking
   - Notification system

3. **Monitoring**
   - Metrics collection
   - Health checks
   - Performance tracking
   - Alert system

### Known Issues

1. **Performance**
   - Cache invalidation timing
   - Batch size optimization
   - Queue depth management
   - Resource utilization

2. **Error Handling**
   - Retry policy tuning
   - Error message clarity
   - Recovery mechanism
   - Notification delivery

3. **Monitoring**
   - Metrics accuracy
   - Alert thresholds
   - Performance baselines
   - Resource limits

### Next Steps

1. **Performance**
   - Implement query optimization
   - Tune cache settings
   - Optimize batch processing
   - Enhance resource management

2. **Error Handling**
   - Improve error recovery
   - Refine retry policies
   - Enhance error tracking
   - Implement notification system

3. **Monitoring**
   - Expand metrics collection
   - Implement health checks
   - Add performance tracking
   - Set up alert system

4. **Documentation**
   - Update architecture docs
   - Add implementation details
   - Include configuration guide
   - Provide troubleshooting guide 