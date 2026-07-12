package com.alessandropesole.bonwoapp.user.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.support.AbstractIntegrationTest;
import com.alessandropesole.bonwoapp.user.domain.model.AccountStatus;
import com.alessandropesole.bonwoapp.user.domain.model.User;
import com.alessandropesole.bonwoapp.user.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserRepositoryAdapter.class)
class UserRepositoryAdapterIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepositoryAdapter userRepository;

    @Test
    void save_persistsAndReloadsUser() {
        User saved = userRepository.save(User.register("user@example.com", "hash", "johndoe"));

        assertThat(saved.getId()).isNotNull();

        User reloaded = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo("user@example.com");
        assertThat(reloaded.getUsername()).isEqualTo("johndoe");
        assertThat(reloaded.getRole()).isEqualTo(UserRole.USER);
        assertThat(reloaded.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void findByEmail_returnsMatchingUser() {
        userRepository.save(User.register("user@example.com", "hash", "johndoe"));

        assertThat(userRepository.findByEmail("user@example.com")).isPresent();
        assertThat(userRepository.findByEmail("unknown@example.com")).isEmpty();
    }

    @Test
    void findByUsername_returnsMatchingUser() {
        userRepository.save(User.register("user@example.com", "hash", "johndoe"));

        assertThat(userRepository.findByUsername("johndoe")).isPresent();
        assertThat(userRepository.findByUsername("unknown")).isEmpty();
    }

    @Test
    void existsByEmailAndUsername_reflectPersistedState() {
        userRepository.save(User.register("user@example.com", "hash", "johndoe"));

        assertThat(userRepository.existsByEmail("user@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("unknown@example.com")).isFalse();
        assertThat(userRepository.existsByUsername("johndoe")).isTrue();
        assertThat(userRepository.existsByUsername("unknown")).isFalse();
    }

    @Test
    void findAll_returnsPagedUsers() {
        userRepository.save(User.register("a@example.com", "hash", "usera"));
        userRepository.save(User.register("b@example.com", "hash", "userb"));

        var page = userRepository.findAll(PageRequest.of(0, 1));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void loadUserByUsername_buildsUnrestrictedDetailsForActiveUser() {
        userRepository.save(User.register("user@example.com", "hash", "johndoe"));

        UserDetails details = userRepository.loadUserByUsername("user@example.com");

        assertThat(details.getUsername()).isEqualTo("user@example.com");
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsername_locksAccountForBannedUser() {
        User user = User.register("user@example.com", "hash", "johndoe");
        user.ban();
        userRepository.save(user);

        UserDetails details = userRepository.loadUserByUsername("user@example.com");

        assertThat(details.isAccountNonLocked()).isFalse();
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsername_disablesAccountForDeletedUser() {
        User user = User.register("user@example.com", "hash", "johndoe");
        User saved = userRepository.save(user);
        saved.softDelete();
        userRepository.save(saved);

        UserDetails details = userRepository.loadUserByUsername(saved.getEmail());

        assertThat(details.isEnabled()).isFalse();
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void loadUserByUsername_throwsWhenEmailNotFound() {
        assertThatThrownBy(() -> userRepository.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
