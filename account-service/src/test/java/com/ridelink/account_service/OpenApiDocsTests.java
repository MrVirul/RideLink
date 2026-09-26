package com.ridelink.account_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Guards the springdoc wiring. Nothing else asserts that the OpenAPI document and the
 * Swagger UI are actually reachable, so a springdoc/Spring Boot incompatibility or a
 * dropped {@code @OpenApiConfig} would otherwise only show up in a browser.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTests {

    @Autowired
    private MockMvc mockMvc;

    private String getOk(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)).andReturn();
        assertEquals(200, result.getResponse().getStatus(), path + " should be served");
        return result.getResponse().getContentAsString();
    }

    @Test
    void specIsServed() throws Exception {
        String spec = getOk("/v3/api-docs");
        assertTrue(spec.contains("RideLink - Account Service API"),
                "the @OpenAPIDefinition title should be in the spec");
    }

    @Test
    void specDeclaresTheBearerSchemeGlobally() throws Exception {
        String spec = getOk("/v3/api-docs");
        assertTrue(spec.contains("\"bearerAuth\""),
                "bearerAuth should be declared in components.securitySchemes");
        assertTrue(spec.contains("\"security\""),
                "spec should carry a security requirement, otherwise the Authorize dialog does nothing");
    }

    @Test
    void specDoesNotLeakTheEntityInternals() throws Exception {
        String spec = getOk("/v3/api-docs");
        assertTrue(spec.contains("UserResponse"), "signup should document the UserResponse shape");
        assertFalse(spec.contains("accountNonLocked"),
                "UserDetails plumbing leaked into the published spec - use UserResponse");
        assertFalse(spec.contains("credentialsNonExpired"),
                "UserDetails plumbing leaked into the published spec - use UserResponse");
    }

    @Test
    void swaggerUiConfigEndpointIsServed() throws Exception {
        String config = getOk("/v3/api-docs/swagger-config");
        assertTrue(config.contains("/v3/api-docs"), "swagger-config should point back at the spec");
    }

    @Test
    void swaggerUiPageIsServed() throws Exception {
        int status = mockMvc.perform(get("/swagger-ui.html")).andReturn().getResponse().getStatus();
        assertTrue(status == 200 || (status >= 300 && status < 400),
                "/swagger-ui.html should render or redirect to the UI, got " + status);
    }
}
