package com.ridelink.account_service.controller;

import com.ridelink.account_service.model.Role;
import com.ridelink.account_service.model.User;
import com.ridelink.account_service.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    public record LoginRequest(
            @Schema(description = "Email address the account was registered with", example = "virul@gmail.com")
            @NotBlank @Email String email,
            @Schema(description = "Account password", example = "Virul@123")
            @NotBlank String password) {}

    public record SignupRequest(
            @Schema(description = "Full name of the account holder", example = "Virul")
            @NotBlank @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters") String name,
            @Schema(description = "Email address used to sign in, must be unique", example = "virul@gmail.com")
            @NotBlank @Email String email,
            @Schema(description = "Account password, minimum 6 characters", example = "Virul@123")
            @NotBlank @Size(min = 6, message = "Password must be at least 6 characters long") String password,
            @Schema(description = "Role the account is created with, one of DRIVER or PASSENGER, defaults to PASSENGER when omitted", example = "DRIVER")
            Role role) {}

    @Schema(name = "UserResponse")
    public record UserResponse(
            @Schema(description = "Id of the created account", example = "1")
            Integer id,
            @Schema(description = "Full name of the account holder", example = "Virul")
            String name,
            @Schema(description = "Email address of the account", example = "virul@gmail.com")
            String email,
            @Schema(description = "Role the account was created with", example = "PASSENGER")
            Role role) {

        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
        }
    }

    @PostMapping("/signup")
    @Operation(summary = "Register a new account")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody SignupRequest request){
        User registeredUser = authService.registerUser(request);
        return ResponseEntity.ok(UserResponse.from(registeredUser));
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange credentials for a JWT")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        String token = authService.authenticateAndGetToken(request.email(), request.password());
        return ResponseEntity.ok(token);
    }
}
