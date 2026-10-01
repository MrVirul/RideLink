package com.ridelink.account_service.controller;

import com.ridelink.account_service.model.User;
import com.ridelink.account_service.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    @Autowired
    private UserService userService;

    public record UpdateProfileRequest(
            @NotBlank 
            @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters") 
            String name) {}

    public record UpdatePasswordRequest(
            @NotBlank String oldPassword, 
            @NotBlank 
            @Size(min = 6, message = "New password must be at least 6 characters long") 
            String newPassword) {}

    @GetMapping("/profile")
    public ResponseEntity<AuthController.UserResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(AuthController.UserResponse.from(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<AuthController.UserResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        User updatedUser = userService.updateUserProfile(userDetails.getUsername(), request.name());
        return ResponseEntity.ok(AuthController.UserResponse.from(updatedUser));
    }

    @PutMapping("/profile/password")
    public ResponseEntity<String> updatePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdatePasswordRequest request) {
        try {
            userService.updatePassword(userDetails.getUsername(), request.oldPassword(), request.newPassword());
            return ResponseEntity.ok("Password updated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
