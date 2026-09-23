package com.ridelink.account_service.service.auth;

import com.ridelink.account_service.cotroller.AuthController;
import com.ridelink.account_service.model.Role;
import com.ridelink.account_service.model.User;
import com.ridelink.account_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtService jwtService;

    public User registerUser(AuthController.SignupRequest request){
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));

        // Set role - default to PASSENGER if not provided
        Role role = request.role() != null ? request.role() : Role.PASSENGER;
        user.setRole(role);

        return userRepository.save(user);
    }

    public String authenticateAndGetToken(String email, String password){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );
        if(authentication.isAuthenticated()) {
            // Load the user to get full details including role
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            // Generate and return JWT token
            return jwtService.generateToken(userDetails);
        } else {
            throw new RuntimeException("Invalid login credentials");
        }
    }
}
