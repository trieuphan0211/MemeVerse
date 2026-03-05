# API Gateway Service

A Spring Cloud Gateway service that acts as a single entry point for the MemeVerse microservices architecture. It provides routing, load balancing, authentication, and service discovery capabilities.

## 🚀 Features

- **API Gateway**: Routes requests to appropriate microservices
- **Service Discovery**: Built-in Eureka Server for service registration
- **Authentication**: Dual security support with OAuth2/JWT and Basic Auth
- **Load Balancing**: Client-side load balancing with Spring Cloud LoadBalancer
- **Route Rewriting**: URL path transformation for clean API endpoints

## 🛠️ Tech Stack

- **Java**: 17
- **Spring Boot**: 4.0.3
- **Spring Cloud**: 2025.1.0
- **Spring Cloud Gateway**: WebMVC implementation
- **Spring Security**: OAuth2 Resource Server + HTTP Basic Auth
- **Netflix Eureka**: Service registry and discovery
- **Logging**: Log4j2
- **Build Tool**: Gradle

## 📋 Prerequisites

- Java 17 or higher
- Gradle 8.x (or use included wrapper)
- Keycloak server (for JWT authentication)
- Running instances of downstream services (user-service, meme-service)

## ⚙️ Configuration

### Application Properties (`application.yml`)

```yaml
server:
  port: 8080

spring:
  application:
    name: API Gateway
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://{keycloak_url}/realms/{realm_name}
          jwk-set-uri: https:/{keycloak_url}/realms/{realm_name}/protocol/openid-connect/certs
```

### Route Configuration

| Route ID | Path | Target Service | Rewrite Pattern |
|----------|------|----------------|-----------------|
| user-service | `/api/v1/user/**` | `lb://user-service` | `/api/v1/user/(?<segment>.*)` → `/api/v1/${segment}` |
| meme-service | `/api/v1/meme/**` | `lb://meme-service` | `/api/v1/meme/(?<segment>.*)` → `/api/v1/${segment}` |

### Security Configuration

#### Eureka Dashboard Access
- **Username**: `{user_name}`
- **Password**: `{password}`
- **Role**: `SUPERADMIN`

#### JWT Authentication
- Configured with Keycloak realm: `MemeVerse`
- Extracts roles from JWT `role` claim
- Supports scope-based authorities with `SCOPE_` prefix
- Supports role-based authorities with `ROLE_` prefix

## 🚀 Getting Started

### Build the Project

```bash
./gradlew clean build
```

### Run the Application

```bash
./gradlew bootRun
```

Or run the JAR directly:

```bash
java -jar build/libs/APIGateway-0.0.1-SNAPSHOT.jar
```

### Access Points

| Endpoint | Description | Authentication                 |
|----------|-------------|--------------------------------|
| `http://localhost:8080` | API Gateway | JWT or Basic                   |
| `http://localhost:8080/eureka` | Eureka Dashboard | Basic Auth (username/password) |
| `http://localhost:8080/api/v1/user/**` | User Service Routes | JWT                            |
| `http://localhost:8080/api/v1/meme/**` | Meme Service Routes | JWT                            |

## 🔒 Security Details

### JWT Token Structure Expected

The gateway expects JWT tokens with the following claims:

```json
{
  "sub": "user-id",
  "role": "USER",
  "scope": "read write"
}
```

### Role Mapping

- JWT `role` claim → `ROLE_{role}` (e.g., `USER` → `ROLE_USER`)
- JWT `scope` claim → `SCOPE_{scope}` (e.g., `read` → `SCOPE_read`)

### Protected Endpoints

| Pattern | Required Role |
|---------|---------------|
| `/eureka/**` | `SUPERADMIN` |
| `/**` | Authenticated |

## 🧪 Testing

Run unit tests:

```bash
./gradlew test
```

## 📁 Project Structure

```
API Gateway/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── gradle/
│   └── wrapper/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── vn/stephenphan/apigateway/
│   │   │       ├── ApiGatewayApplication.java
│   │   │       └── configuration/
│   │   │           └── SecurityConfig.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/
│           └── vn/stephenphan/apigateway/
│               └── ApiGatewayApplicationTests.java
└── build/
```

## 📦 Dependencies

Key dependencies used in this project:

| Dependency | Purpose |
|------------|---------|
| `spring-cloud-starter-gateway-server-webmvc` | API Gateway WebMVC implementation |
| `spring-cloud-starter-netflix-eureka-server` | Service registry |
| `spring-boot-starter-security` | Security framework |
| `spring-boot-starter-security-oauth2-resource-server` | JWT validation |
| `spring-boot-starter-log4j2` | Logging framework |
| `lombok` | Boilerplate code reduction |

## 📝 License

This project is part of the MemeVerse microservices ecosystem.

## 👤 Author

Stephen Phan - [stephenphan.io.vn](https://stephenphan.io.vn)

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
