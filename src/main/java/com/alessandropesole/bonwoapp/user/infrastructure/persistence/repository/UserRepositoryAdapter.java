package com.alessandropesole.bonwoapp.user.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.user.domain.model.AccountStatus;
import com.alessandropesole.bonwoapp.user.domain.model.User;
import com.alessandropesole.bonwoapp.user.domain.port.out.UserRepository;
import com.alessandropesole.bonwoapp.user.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository, UserDetailsService {

    private final UserJpaRepository jpa;

    @Override
    public User save(User user) {
        return UserMapper.toDomain(jpa.save(UserMapper.toEntity(user)));
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpa.findByUsername(username).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpa.existsByUsername(username);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return jpa.findAll(pageable).map(UserMapper::toDomain);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return jpa.findByEmail(email)
                .map(e -> org.springframework.security.core.userdetails.User.builder()
                        .username(e.getEmail())
                        .password(e.getPasswordHash())
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + e.getRole().name())))
                        .accountLocked(e.getStatus() == AccountStatus.BANNED)
                        .disabled(e.getStatus() == AccountStatus.DELETED)
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
