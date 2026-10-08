package com.ridelink.account_service;

import com.ridelink.account_service.model.Role;
import com.ridelink.account_service.repository.UserRepository;
import com.ridelink.account_service.service.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins the error contract of the auth endpoints: duplicate signups must be 409, bad
 * credentials 401, malformed payloads 400 - all as the shared ApiErrorResponse JSON,
 * never as a whitelabel 500 or a raw text/plain body.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthExceptionHandlingTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @BeforeEach
    public void cleanup() {
        userRepository.deleteAll();
    }

    private String signupBody(String name, String email, String password, String role) {
        return "{\"name\":\"" + name + "\",\"email\":\"" + email
                + "\",\"password\":\"" + password + "\""
                + (role == null ? "" : ",\"role\":\"" + role + "\"") + "}";
    }

    @Test
    public void duplicateSignupReturns409() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("Dup User", "dup@example.com", "password123", "PASSENGER")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("Dup User Again", "dup@example.com", "password123", "DRIVER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("An account with this email already exists"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/signup"));
    }

    @Test
    public void signupWithBlankFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("", "", "123", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/auth/signup"));
    }

    @Test
    public void signupWithUnknownRoleReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("Bad Role", "badrole@example.com", "password123", "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/signup"));
    }

    @Test
    public void signupWithMalformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    public void loginWithWrongPasswordReturns401() throws Exception {
        authService.registerUser(new com.ridelink.account_service.controller.AuthController.SignupRequest(
                "Login User", "login@example.com", "password123", Role.PASSENGER));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("Login User", "login@example.com", "wrong-password", null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
    }

    @Test
    public void loginWithUnknownEmailReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("Nobody", "nobody@example.com", "password123", null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
    }

    @Test
    public void loginWithBlankEmailReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
    }

    @Test
    public void successfulSignupStillReturns201WithUserResponse() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("New User", "new@example.com", "password123", "DRIVER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.role").value("DRIVER"))
                .andExpect(jsonPath("$.id").isNumber());
    }
}
