package com.example.resourcebooking.config;

import com.example.resourcebooking.entity.User;
import com.example.resourcebooking.enums.Role;
import com.example.resourcebooking.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedUser("admin", "admin123", Role.ADMIN);
        seedUser("user", "user123", Role.USER);
    }

    private void seedUser(String username, String rawPassword, Role role) {
        if (!userRepository.existsByUsername(username)) {
            userRepository.save(new User(username, passwordEncoder.encode(rawPassword), role));
        }
    }
}
