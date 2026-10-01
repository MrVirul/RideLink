package com.ridelink.account_service.service;

import com.ridelink.account_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    public com.ridelink.account_service.model.User getUserProfile(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    public com.ridelink.account_service.model.User updateUserProfile(String email, String newName) {
        com.ridelink.account_service.model.User user = getUserProfile(email);
        if (newName != null && !newName.trim().isEmpty()) {
            user.setName(newName);
            log.info("AUDIT: User profile updated for email: {}", email);
        }
        return userRepository.save(user);
    }

    public void updatePassword(String email, String oldPassword, String newPassword) {
        com.ridelink.account_service.model.User user = getUserProfile(email);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("AUDIT: Failed password update attempt for email: {}", email);
            throw new RuntimeException("Incorrect old password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("AUDIT: Password successfully updated for email: {}", email);
    }
}
