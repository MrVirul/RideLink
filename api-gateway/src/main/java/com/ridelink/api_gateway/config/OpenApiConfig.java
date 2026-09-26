package com.ridelink.api_gateway.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "RideLink API Gateway",
                version = "v1",
                description = """
                        Edge router for the RideLink platform. This document only describes the
                        gateway itself (actuator endpoints) - it is not the place to browse the
                        business APIs.

                        Use the Swagger UI at `/swagger-ui.html` instead: its dropdown aggregates
                        the spec of every service. Each service spec is fetched through this
                        gateway and advertises its own `servers` entry, so "Try it out" calls
                        back through here.
                        """,
                contact = @Contact(name = "RideLink", url = "https://github.com/MrVirul/RideLink")
        ),
        servers = @Server(
                url = "${app.openapi.servers.base-url}",
                description = "RideLink API gateway"
        )
)
public class OpenApiConfig {
}
