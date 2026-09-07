package com.alessandropesole.bonwoapp.user.domain.model;

import com.alessandropesole.bonwoapp.user.domain.exception.AlreadyBannedException;
import com.alessandropesole.bonwoapp.user.domain.exception.InvalidEmailException;
import com.alessandropesole.bonwoapp.user.domain.exception.InvalidUsernameException;
import com.alessandropesole.bonwoapp.user.domain.exception.NotBannedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"not-an-email", "missing-at-sign.com", "user@", "@domain.com"})
    void register_rejectsInvalidEmail(String invalidEmail) {
        assertThatThrownBy(() -> User.register(invalidEmail, "hash", "johndoe"))
                .isInstanceOf(InvalidEmailException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "invalid username", "invalid-username!"})
    void register_rejectsInvalidUsername(String invalidUsername) {
        assertThatThrownBy(() -> User.register("user@example.com", "hash", invalidUsername))
                .isInstanceOf(InvalidUsernameException.class);
    }

    @Test
    void register_rejectsUsernameLongerThan30Characters() {
        String tooLong = "a".repeat(31);

        assertThatThrownBy(() -> User.register("user@example.com", "hash", tooLong))
                .isInstanceOf(InvalidUsernameException.class);
    }

    @Test
    void register_rejectsUsernameShorterThan3Characters() {
        assertThatThrownBy(() -> User.register("user@example.com", "hash", "ab"))
                .isInstanceOf(InvalidUsernameException.class);
    }

    @Test
    void updateUsername_setsNewUsername() {
        User user = User.register("user@example.com", "hash", "johndoe");

        user.updateUsername("janedoe");

        assertThat(user.getUsername()).isEqualTo("janedoe");
    }

    @Test
    void updateUsername_rejectsUsernameWithSurroundingWhitespace() {
        User user = User.register("user@example.com", "hash", "johndoe");

        assertThatThrownBy(() -> user.updateUsername("  janedoe  "))
                .isInstanceOf(InvalidUsernameException.class);
    }

    @Test
    void updateUsername_rejectsInvalidNewUsername() {
        User user = User.register("user@example.com", "hash", "johndoe");

        assertThatThrownBy(() -> user.updateUsername("invalid username!"))
                .isInstanceOf(InvalidUsernameException.class);
    }

    @Test
    void updateAvatarId_delegatesToProfileWithAvatarId() {
        User user = User.register("user@example.com", "hash", "johndoe");

        user.updateAvatarId(42L);

        assertThat(user.getProfile().getAvatarId()).isEqualTo(42L);
    }

    @Test
    void ban_setsStatusToBanned() {
        User user = User.register("user@example.com", "hash", "johndoe");

        user.ban();

        assertThat(user.getStatus()).isEqualTo(AccountStatus.BANNED);
    }

    @Test
    void ban_throwsWhenAlreadyBanned() {
        User user = User.register("user@example.com", "hash", "johndoe");
        user.ban();

        assertThatThrownBy(user::ban).isInstanceOf(AlreadyBannedException.class);
    }

    @Test
    void unban_restoresActiveStatus() {
        User user = User.register("user@example.com", "hash", "johndoe");
        user.ban();

        user.unban();

        assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void unban_throwsWhenNotBanned() {
        User user = User.register("user@example.com", "hash", "johndoe");

        assertThatThrownBy(user::unban).isInstanceOf(NotBannedException.class);
    }

    @Test
    void changeRole_worksOnBannedAccount() {
        User user = User.reconstitute(1L, "user@example.com", "hash", "johndoe",
                UserRole.USER, AccountStatus.BANNED, UserProfile.empty(), null);

        user.changeRole(UserRole.ADMIN);

        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void isActive_trueOnlyForActiveStatus() {
        User user = User.register("user@example.com", "hash", "johndoe");
        assertThat(user.isActive()).isTrue();

        user.ban();
        assertThat(user.isActive()).isFalse();
    }

    @Test
    void isAdmin_trueOnlyForAdminRole() {
        User user = User.register("user@example.com", "hash", "johndoe");
        assertThat(user.isAdmin()).isFalse();

        user.changeRole(UserRole.ADMIN);
        assertThat(user.isAdmin()).isTrue();
    }
}
