package com.ridelink.fare_service.config;

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
                title = "RideLink - Fare Service API",
                version = "v1",
                description = """
                        Fare calculation, ride receipts and payment processing.

                        This service is scaffolded: it has no business endpoints yet, so the
                        spec below only carries the actuator endpoints. Endpoints are
                        documented automatically as they are added - no manual spec file
                        to maintain.

                        Operations inherit the `bearerAuth` requirement from this definition,
                        so new endpoints are secured in the spec by default. A public endpoint
                        opts out with an empty `@SecurityRequirements`.
                        """,
                contact = @Contact(name = "RideLink", url = "https://github.com/MrVirul/RideLink")
        ),
        servers = @Server(
                url = "${app.openapi.servers.base-url}",
                description = "RideLink API gateway"
        ),
        // Spec-wide default: without this, no operation carries a security requirement and
        // swagger-ui never attaches the token entered in the Authorize dialog.
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = """
                JWT issued by the account service (`POST /api/v1/auth/login`).
                Paste the raw token only.

                Note: this service does not enforce authentication yet - the
                `hasRole("PASSENGER")` rule currently lives in the account service.
                The scheme is declared here so endpoints are ready for the gateway
                JWT filter (issue #36).
                """
)
public class OpenApiConfig {
}
