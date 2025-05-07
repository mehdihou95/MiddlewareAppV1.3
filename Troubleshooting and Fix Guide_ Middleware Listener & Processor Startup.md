# Troubleshooting and Fix Guide: Middleware Listener & Processor Startup

This guide addresses potential issues preventing the backend listener and processor Spring Boot applications from running correctly, especially in a hybrid environment with Dockerized services (Redis, DB, RabbitMQ) and applications running locally.

## 1. General Checks

1.  **Java Version:** Ensure the Java version used to run the listener and processor applications is compatible with the project's requirements (e.g., Java 17 as per previous knowledge).
2.  **Build Success:** Confirm that both `listener` and `processor` modules, along with `shared-config`, build successfully using Maven or Gradle without errors. Clean the build (`mvn clean install` or `gradle clean build`) to ensure no stale artifacts are causing issues.
3.  **Dependencies:** Verify that all necessary dependencies are correctly defined in the `pom.xml` or `build.gradle` files for each module.

## 2. Connectivity to Dockerized Services

When running Spring Boot applications locally and connecting to services in Docker containers:

*   **Hostname:** Always use `localhost` (or `127.0.0.1`) in your `application.yml` files to connect to services exposed by Docker on your host machine. Do **not** use Docker service names (e.g., `rabbitmq`, `db`) as these are typically resolvable only within the Docker network.
*   **Port Mappings:** Double-check that the ports mapped in your `docker-compose.yml` match the ports configured in your `application.yml` files.
    *   RabbitMQ: Docker `5672:5672` -> YAML `spring.rabbitmq.port: 5672` (Correct)
    *   Redis: Docker `6379:6379` -> YAML `spring.redis.port: 6379` (Correct for processor)
    *   PostgreSQL: Docker `5433:5432` -> YAML `spring.datasource.url: jdbc:postgresql://localhost:5433/...` (Correct)
*   **Firewall:** Ensure no local firewall is blocking connections from your Spring Boot applications to these `localhost` ports.
*   **Service Health:** Verify that all Docker containers (RabbitMQ, Redis, DB) are running and healthy. Check `docker ps` and `docker logs <container_name>`.

## 3. YAML Configuration Analysis (`application.yml`, `application-shared.yml`)

Your YAML configurations for service connectivity (`localhost`, ports, basic credentials) appear largely correct based on the review. However, the following specific integration issues are highly probable causes for startup failures, particularly for the `listener` application.

### Issue 1: Listener Application - Redis Session Conflict (High Probability)

*   **Problem:**
    *   The `shared-config/src/main/resources/application-shared.yml` file configures Spring Session to use Redis by default: `spring.session.store-type: redis`.
    *   The `listener/src/main/resources/application.yml` includes the `shared` profile, thereby inheriting this Redis session configuration.
    *   However, the listener's `application.yml` also explicitly disables Redis: `spring.redis.enabled: false` and excludes Redis auto-configurations (`org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration`, `org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration`).
*   **Symptom:** The listener application will likely fail during startup when Spring Session attempts to create beans for Redis-backed session management (e.g., `RedisIndexedSessionRepository`) but cannot find a configured `RedisConnectionFactory` or other necessary Redis beans due to Redis being disabled.
*   **Fixes (Choose one based on listener's session requirements):**
    1.  **Disable Spring Session for Listener:** If the listener is stateless or does not require HTTP sessions, explicitly disable Spring Session in `listener/src/main/resources/application.yml`:
        ```yaml
        spring:
          session:
            store-type: none
        ```
    2.  **Use a Different Session Store for Listener:** If the listener needs sessions but cannot use Redis, configure an alternative store (e.g., `cookie` for simple cases, or JDBC if appropriate, though this adds DB load):
        ```yaml
        spring:
          session:
            store-type: cookie # Or jdbc, etc.
        ```
    3.  **Conditional Session Configuration (More Advanced):** Modify `shared-config` to conditionally configure the session store based on a property or profile, allowing modules to opt-out of Redis sessions more cleanly.

### Issue 2: Listener Application - SFTP/AS2 Service Dependencies (High Probability)

*   **Problem:**
    *   The `listener/src/main/resources/application.yml` contains extensive configurations for SFTP (`sftp.enabled: true`, `sftp.host: localhost`, `sftp.port: 22`, etc.) and AS2.
    *   These configurations imply that the listener application either provides or connects to SFTP/AS2 services.
    *   The current `deploy/docker-compose.yml` does **not** define an SFTP service container.
*   **Symptom:** If the listener attempts to initialize Camel routes, beans, or components related to SFTP (e.g., trying to connect to `localhost:22` for SFTP) and the SFTP server is not running on the host machine where the listener application is executed, the application will fail to start. Similar issues can occur with AS2 configuration if dependencies are not met (e.g., missing keystores at `keystore.jks`).
*   **Fixes:**
    1.  **Ensure SFTP Server Availability:** If the listener needs to connect to an SFTP server on `localhost:22` (as per `sftp.host: localhost`), ensure an SFTP server is running on the machine where you are executing the listener Spring Boot application and is accessible on that port with the configured credentials (`admin/admin`).
    2.  **Provide SFTP via Docker:** If the SFTP server is meant to be part of your Dockerized environment, add an SFTP service (e.g., `atmoz/sftp`) to your `docker-compose.yml` and ensure the listener's `application.yml` points to it correctly (e.g., `localhost` and the mapped port).
    3.  **Disable SFTP/AS2 for Testing:** If SFTP/AS2 functionality is not critical for the current task of getting the listener and processor running, temporarily disable these components in `listener/application.yml` to isolate the problem:
        ```yaml
        sftp:
          enabled: false
        # Potentially comment out or disable AS2 related beans/routes if they cause startup issues
        ```
        You might also need to conditionally enable Camel routes or beans related to SFTP/AS2 based on these properties.
    4.  **Verify AS2 Keystore:** Ensure the `keystore.jks` file is present at the expected location relative to the listener's runtime path and the passwords are correct.

## 4. Other Potential Discrepancies and Checks

*   **Spring Profiles:** Both listener and processor `application.yml` include `spring.profiles.include: shared`. Ensure this is intended and that properties from `application-shared.yml` are being loaded and overridden correctly where needed. The `spring.main.allow-bean-definition-overriding: true` is set, which allows overriding but can sometimes mask configuration issues.
*   **JPA L2 Cache Configuration:**
    *   `shared-config/application-shared.yml` configures JPA L2 cache with `org.hibernate.cache.jcache.internal.JCacheRegionFactory` and `provider_class: org.ehcache.jsr107.EhcacheCachingProvider`.
    *   `listener/application.yml` also specifies these exact same JCache/Ehcache provider settings under `spring.jpa.properties.hibernate.cache`.
    *   **Redundancy/Clarity:** This is redundant. The configuration in `application-shared.yml` should be sufficient. While not a direct cause of failure, it's an inconsistency. It's better to define shared configurations in one place. The listener's specific JPA cache settings can be removed if they are identical to shared.
*   **Flyway Configuration:** Both processor and shared YAMLs explicitly set `flyway.url`, `user`, and `password`. The listener YAML does not, relying on `spring.datasource.*` properties. This is generally fine as Spring Boot can infer Flyway's datasource. All configurations correctly point to `jdbc:postgresql://localhost:5433/middleware_config`. Ensure the `middleware_config` database and `middleware_user` exist in your PostgreSQL container as per `deploy/database/setup-database.sql` and `docker-compose.yml`.
*   **Logging:** Increase log levels to `DEBUG` or `TRACE` for problematic components (e.g., `org.springframework.boot.autoconfigure`, `org.springframework.orm.jpa`, `org.hibernate`, `org.apache.camel`, `com.jcraft.jsch` for SFTP) in both listener and processor `application.yml` files. Examine the full startup logs carefully for specific error messages and stack traces. These logs are crucial for pinpointing the exact point of failure.

## 5. How to Run and Debug

1.  **Start Docker Services:** `docker-compose -f deploy/docker-compose.yml up -d`
2.  **Run Processor:** Execute the processor Spring Boot application (e.g., from your IDE or via `mvn spring-boot:run` or `java -jar ...jar`). Check its console output for errors.
3.  **Run Listener:** Execute the listener Spring Boot application. Check its console output for errors.

By addressing the high-probability issues (Listener Redis Session conflict, Listener SFTP/AS2 dependencies) and reviewing logs, you should be able to identify and resolve the startup problems.

