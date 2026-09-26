package com.ridelink.account_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "RideLink - Account Service API",
                version = "v1",
                description = """
                        Registration and authentication for RideLink accounts.

                        Roles: `DRIVER` and `PASSENGER`. A successful `POST /api/v1/auth/login`
                        returns a raw JWT string (not a JSON object) with a 24 hour expiry.
                        Paste that value into the **Authorize** dialog to call protected endpoints -
                        Swagger UI adds the `Bearer ` prefix for you, so do not include it yourself.

                        Every operation is documented as requiring `bearerAuth` unless it opts out.
                        The two public operations under `/api/v1/auth` opt out, so they show no
                        lock icon; add `@SecurityRequirements` (empty) to any new public
                        operation, or `@SecurityRequirement(name = "bearerAuth")` to restate it.
                        """,
                contact = @Contact(name = "RideLink", url = "https://github.com/MrVirul/RideLink")
        ),
        servers = @Server(
                url = "${app.openapi.servers.base-url}",
                description = "RideLink API gateway"
        ),
        // Applying the scheme to the whole spec is what makes the Authorize dialog do
        // something: swagger-ui only attaches the stored token to operations that declare a
        // security requirement, and per-operation annotations alone left every operation
        // unsecured. Public operations opt out with an empty @SecurityRequirements.
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "JWT issued by `POST /api/v1/auth/login`. Paste the raw token only."
)
public class OpenApiConfig {
}
