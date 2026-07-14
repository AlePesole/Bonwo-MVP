package com.alessandropesole.bonwoapp.user.infrastructure.config;

import com.alessandropesole.bonwoapp.user.domain.model.User;
import com.alessandropesole.bonwoapp.user.domain.model.UserRole;
import com.alessandropesole.bonwoapp.user.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties properties;

    @Override
    public void run(String... args) {
        String email = properties.email();
        String username = properties.username();
        String password = properties.password();

        if (isBlank(email) || isBlank(username) || isBlank(password)) {
            return;
        }
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User admin = User.register(email, passwordEncoder.encode(password), username);
        admin.changeRole(UserRole.ADMIN);
        userRepository.save(admin);
        log.info("Seeded initial admin user: {}", email);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}