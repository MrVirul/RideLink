package com.ridelink.account_service;

import com.ridelink.account_service.cotroller.AuthController;
import com.ridelink.account_service.model.Role;
import com.ridelink.account_service.model.User;
import com.ridelink.account_service.repository.UserRepository;
import com.ridelink.account_service.service.auth.AuthService;
import com.ridelink.account_service.service.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RoleBasedAccessTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void cleanup() {
        userRepository.deleteAll();
    }

    @Test
    public void testSignupAsDriver() {
        AuthController.SignupRequest request = new AuthController.SignupRequest(
                "Driver User",
                "driver@example.com",
                "password123",
                Role.DRIVER
        );

        User registeredUser = authService.registerUser(request);

        assertNotNull(registeredUser);
        assertEquals("driver@example.com", registeredUser.getEmail());
        assertEquals(Role.DRIVER, registeredUser.getRole());

        // Verify authorities
        Collection<? extends GrantedAuthority> authorities = registeredUser.getAuthorities();
        assertTrue(authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_DRIVER")));
    }

    @Test
    public void testSignupAsPassenger() {
        AuthController.SignupRequest request = new AuthController.SignupRequest(
                "Passenger User",
                "passenger@example.com",
                "password123",
                Role.PASSENGER
        );

        User registeredUser = authService.registerUser(request);

        assertNotNull(registeredUser);
        assertEquals("passenger@example.com", registeredUser.getEmail());
        assertEquals(Role.PASSENGER, registeredUser.getRole());

        // Verify authorities
        Collection<? extends GrantedAuthority> authorities = registeredUser.getAuthorities();
        assertTrue(authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_PASSENGER")));
    }

    @Test
    public void testSignupDefaultRole() {
        AuthController.SignupRequest request = new AuthController.SignupRequest(
                "Default User",
                "default@example.com",
                "password123",
                null  // No role specified
        );

        User registeredUser = authService.registerUser(request);

        assertNotNull(registeredUser);
        assertEquals("default@example.com", registeredUser.getEmail());
        assertEquals(Role.PASSENGER, registeredUser.getRole()); // Should default to PASSENGER
    }

    @Test
    public void testJwtIncludesRole() {
        // Create a user
        AuthController.SignupRequest request = new AuthController.SignupRequest(
                "JWT Test User",
                "jwt@example.com",
                "password123",
                Role.DRIVER
        );
        User registeredUser = authService.registerUser(request);

        // Generate token
        String token = jwtService.generateToken(registeredUser);

        assertNotNull(token);

        // Extract role from token
        String roleFromToken = jwtService.extractRole(token);
        assertEquals("DRIVER", roleFromToken);
    }

    @Test
    public void testAuthenticationGeneratesJwtToken() {
        // Create a user
        AuthController.SignupRequest request = new AuthController.SignupRequest(
                "Auth Test User",
                "auth@example.com",
                "password123",
                Role.DRIVER
        );
        authService.registerUser(request);

        // Authenticate and get token
        String token = authService.authenticateAndGetToken("auth@example.com", "password123");

        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Verify token is valid
        String extractedEmail = jwtService.extractUsername(token);
        assertEquals("auth@example.com", extractedEmail);

        // Verify token contains role
        String extractedRole = jwtService.extractRole(token);
        assertEquals("DRIVER", extractedRole);
    }

    @Test
    public void testUserAuthorityBasedOnRole() {
        // Create driver user
        AuthController.SignupRequest driverRequest = new AuthController.SignupRequest(
                "Driver", "driver@test.com", "pass123", Role.DRIVER
        );
        User driver = authService.registerUser(driverRequest);

        // Create passenger user
        AuthController.SignupRequest passengerRequest = new AuthController.SignupRequest(
                "Passenger", "passenger@test.com", "pass123", Role.PASSENGER
        );
        User passenger = authService.registerUser(passengerRequest);

        // Verify driver has ROLE_DRIVER authority
        assertTrue(driver.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_DRIVER")));
        assertFalse(driver.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_PASSENGER")));

        // Verify passenger has ROLE_PASSENGER authority
        assertTrue(passenger.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_PASSENGER")));
        assertFalse(passenger.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_DRIVER")));
    }
}

