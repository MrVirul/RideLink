package com.ridelink.account_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

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

                        Every operation is documented as requiring `bearerAuth` unless it is listed
                        as public in `OpenApiConfig.PUBLIC_PATHS`, so they show no lock icon.
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
        // unsecured.
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

    /**
     * Operations that {@code SecurityConfig} exposes with {@code permitAll}, so they must not
     * inherit the spec-wide {@code bearerAuth} requirement. Add a path here when a new public
     * endpoint is introduced.
     */
    static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/signup",
            "/api/v1/auth/login"
    );

    /**
     * Clears the inherited security requirement on {@link #PUBLIC_PATHS}.
     *
     * <p>This lives here rather than on the controller methods so the controller stays free of
     * Swagger annotations: an empty {@code @SecurityRequirements} on each method would work
     * too, but it spreads API-documentation concerns through the business code, and forgetting
     * it on a new endpoint silently mislabels that endpoint as secured.
     */
    @Bean
    public OpenApiCustomizer publicEndpointsOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().entrySet().stream()
                    .filter(entry -> PUBLIC_PATHS.contains(entry.getKey()))
                    .forEach(entry -> entry.getValue().readOperations()
                            .forEach(operation -> operation.setSecurity(List.of())));
        };
    }
}
