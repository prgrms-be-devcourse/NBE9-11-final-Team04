package com.team04.global.config;

import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.init.email}")
    private String email;

    @Value("${admin.init.password}")
    private String password;

    @Value("${admin.init.name}")
    private String name;

    @Value("${admin.init.nickname}")
    private String nickname;

    @Value("${admin.init.age}")
    private int age;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(email)) {
            log.info("Admin account already exists: {}", email);
            return;
        }

        User admin = User.create(email, passwordEncoder.encode(password), name, nickname, age, Role.ADMIN);
        userRepository.save(admin);
        log.info("Initial admin account created: {}", email);
    }
}
