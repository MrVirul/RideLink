package com.ridelink.account_service.cotroller;

import com.ridelink.account_service.model.User;
import com.ridelink.account_service.service.auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    public record LoginRequest(String email, String password) {}

    @PostMapping("/signup")
    public ResponseEntity<User> register(@RequestBody User user){
        User registeredUser = authService.registerUser(user);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        String token = authService.authenticateAndGetToken(request.email(), request.password());
        return ResponseEntity.ok(token);
    }
}
