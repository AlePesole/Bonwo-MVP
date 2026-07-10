package com.alessandropesole.bonwoapp.user.application.service;

import com.alessandropesole.bonwoapp.user.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final UserRepository userRepository;

    public Long resolveId(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found in DB: " + principal.getUsername()))
                .getId();
    }
}
