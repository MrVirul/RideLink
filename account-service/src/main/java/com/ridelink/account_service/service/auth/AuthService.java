package com.ridelink.account_service.service.auth;

import com.ridelink.account_service.controller.AuthController;
import com.ridelink.account_service.exception.EmailAlreadyExistsException;
import com.ridelink.account_service.exception.InvalidCredentialsException;
import com.ridelink.account_service.model.Role;
import com.ridelink.account_service.model.User;
import com.ridelink.account_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        if (userRepository.findByEmail(request.email()).isPresent()) {
            log.warn("AUDIT: Rejected signup attempt with duplicate email: {}", request.email());
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));

        // Set role - default to PASSENGER if not provided
        Role role = request.role() != null ? request.role() : Role.PASSENGER;
        user.setRole(role);

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Race: a concurrent signup claimed the email between the check above and this save
            log.warn("AUDIT: Rejected signup attempt with duplicate email (race): {}", request.email());
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }
        log.info("AUDIT: New user registered with email: {}, role: {}", savedUser.getEmail(), savedUser.getRole());
        return savedUser;
    }

    public String authenticateAndGetToken(String email, String password){
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
        } catch (AuthenticationException e) {
            log.warn("AUDIT: Failed login attempt for email: {}", email);
            throw new InvalidCredentialsException("Invalid email or password", e);
        }
        // Load the user to get full details including role
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        log.info("AUDIT: Successful login for email: {}", email);
        // Generate and return JWT token
        return jwtService.generateToken(userDetails);
    }
}
