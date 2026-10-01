# Account Service

## Overall Project Idea
RideLink is a backend-only ride-hailing platform consisting of several independently deployable Spring Boot microservices. These services register with a Netflix Eureka service registry and are accessed through a single API Gateway. 

The `account-service` is a core microservice in this architecture. Its primary responsibility is managing user identities, profiles, and authentication for the platform, ensuring secure access to other services in the RideLink ecosystem.

## Current Stage
- **Fully Implemented**: The `account-service` is currently the most complete service in the repository.
- **Authentication**: JWT-based authentication is fully integrated using Spring Security.
- **User Profile Management**: Includes endpoints for users to view their profile, update their details, and securely change their password.
- **Database Integration**: Connects to a dedicated PostgreSQL database (configured via environment variables).
- **Service Discovery**: Registers with the Eureka service registry on port `8081`.
- **API Documentation**: OpenAPI 3 specification and Swagger UI are dynamically generated and available.

## Rules
- **Coding Standards**: Follow standard Java 21 and Spring Boot 3/4 best practices.
- **Security First**: Do not commit secrets (`env.properties`). Any new authentication features must be thoroughly tested.
- **Statelessness**: The service should remain stateless to allow horizontal scaling; rely on JWTs for session state.
- **Documentation**: Any new endpoints must be documented using `springdoc-openapi` annotations.
- **Contributions**: Follow the guidelines in the root `CONTRIBUTING.md` (GitHub Flow, Conventional Commits).

## Enhancements
Here are some potential areas for development and enhancement in the `account-service`:
1. **OAuth2 / Social Login**: Integrate Google/Facebook login alongside email/password authentication.
2. **Password Management**: Implement "Forgot Password" functionality with secure token-based email reset links.
3. **Email Verification**: Add a flow to verify user email addresses upon registration.
4. **Role-Based Access Control (RBAC)**: Expand JWT claims to include fine-grained roles (e.g., Rider, Driver, Admin) and enforce them across endpoints.
5. **Caching**: Introduce Redis to cache frequently accessed user profiles and reduce database load.
6. **Multi-Factor Authentication (MFA)**: Add an extra layer of security for user logins.
7. **Rate Limiting**: Implement login attempt rate limiting to prevent brute-force attacks.
