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
                contact = @Contact(name = "RideLink", url = "https://github.com/MrVirul/RideLink")
        ),
        servers = @Server(url = "${app.openapi.servers.base-url}")
)
public class OpenApiConfig {
}
