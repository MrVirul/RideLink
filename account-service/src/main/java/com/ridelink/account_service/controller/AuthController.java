package com.ridelink.account_service.controller;

import com.ridelink.account_service.model.Role;
import com.ridelink.account_service.model.User;
import com.ridelink.account_service.service.auth.AuthService;
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
            @NotBlank @Email String email, 
            @NotBlank String password) {}

    public record SignupRequest(
            @NotBlank @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters") String name, 
            @NotBlank @Email String email, 
            @NotBlank @Size(min = 6, message = "Password must be at least 6 characters long") String password, 
            Role role) {}

    public record UserResponse(Integer id, String name, String email, Role role) {

        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody SignupRequest request){
        User registeredUser = authService.registerUser(request);
        return ResponseEntity.ok(UserResponse.from(registeredUser));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        String token = authService.authenticateAndGetToken(request.email(), request.password());
        return ResponseEntity.ok(token);
    }
}
