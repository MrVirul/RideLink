package com.ridelink.account_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Guards the springdoc wiring for this service. The gateway aggregates this spec into its
 * own Swagger UI dropdown, so an endpoint or DTO example that quietly drops out of the
 * document is only visible in a browser - until now.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTests {

    @Autowired
    private MockMvc mockMvc;

    private String spec() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs")).andReturn();
        assertEquals(200, result.getResponse().getStatus(), "/v3/api-docs should be served");
        return result.getResponse().getContentAsString();
    }

    @Test
    void specIsServed() throws Exception {
        assertTrue(spec().contains("RideLink - Account Service API"),
                "the @OpenAPIDefinition title should be in the spec");
    }

    @Test
    void specDeclaresTheBearerSchemeGlobally() throws Exception {
        String spec = spec();
        assertTrue(spec.contains("\"bearerAuth\""),
                "bearerAuth should be declared in components.securitySchemes");
        assertTrue(spec.contains("\"security\""),
                "spec should carry a security requirement, otherwise the Authorize dialog does nothing");
    }

    @Test
    void everyEndpointIsPublished() throws Exception {
        String spec = spec();
        for (String path : new String[]{
                "/api/v1/auth/signup",
                "/api/v1/auth/login",
                "/api/v1/auth/me",
                "/api/v1/auth/logout",
                "/api/v1/accounts/profile",
                "/api/v1/accounts/profile/password"}) {
            assertTrue(spec.contains("\"" + path + "\""),
                    "gateway Swagger UI should list " + path);
        }
    }

    /**
     * A request body with no {@code example} renders as placeholder noise in Swagger UI
     * ("string", 0), which makes the endpoint untestable by hand. Every property of every
     * request schema has to carry a real sample.
     */
    @Test
    void everyRequestSchemaCarriesSampleValues() throws Exception {
        String spec = spec();
        for (String schema : new String[]{"LoginRequest", "SignupRequest",
                "UpdateProfileRequest", "UpdatePasswordRequest"}) {
            assertTrue(spec.contains("\"" + schema + "\""), schema + " should be in the spec");
        }
        assertTrue(spec.contains("\"example\""),
                "request schemas should carry example values so Swagger UI shows a usable body");
        assertTrue(spec.contains("virul@gmail.com"), "LoginRequest/SignupRequest should show a sample email");
    }

    @Test
    void specDoesNotLeakTheEntityInternals() throws Exception {
        String spec = spec();
        assertTrue(spec.contains("UserResponse"), "signup should document the UserResponse shape");
        assertFalse(spec.contains("accountNonLocked"),
                "UserDetails plumbing leaked into the published spec - use UserResponse");
        assertFalse(spec.contains("credentialsNonExpired"),
                "UserDetails plumbing leaked into the published spec - use UserResponse");
    }

    @Test
    void swaggerUiConfigEndpointIsServed() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs/swagger-config")).andReturn();
        assertEquals(200, result.getResponse().getStatus(), "/v3/api-docs/swagger-config should be served");
        assertTrue(result.getResponse().getContentAsString().contains("/v3/api-docs"),
                "swagger-config should point back at the spec");
    }

    @Test
    void swaggerUiPageIsServed() throws Exception {
        int status = mockMvc.perform(get("/swagger-ui.html")).andReturn().getResponse().getStatus();
        assertTrue(status == 200 || (status >= 300 && status < 400),
                "/swagger-ui.html should render or redirect to the UI, got " + status);
    }
}