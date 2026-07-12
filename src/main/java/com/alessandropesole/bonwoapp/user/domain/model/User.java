package com.alessandropesole.bonwoapp.user.domain.model;

import com.alessandropesole.bonwoapp.shared.domain.AggregateRoot;
import com.alessandropesole.bonwoapp.user.domain.exception.AccountDeletedException;
import com.alessandropesole.bonwoapp.user.domain.exception.AlreadyBannedException;
import com.alessandropesole.bonwoapp.user.domain.exception.InvalidEmailException;
import com.alessandropesole.bonwoapp.user.domain.exception.InvalidUsernameException;
import com.alessandropesole.bonwoapp.user.domain.exception.NotBannedException;
import lombok.Getter;

import java.time.Instant;

@Getter
public class User extends AggregateRoot {

    private Long id;
    private String email;
    private String passwordHash;
    private String username;
    private UserRole role;
    private AccountStatus status;
    private UserProfile profile;
    private Instant createdAt;

    public static User register(String email, String passwordHash, String username) {
        validateEmail(email);
        validateUsername(username);

        User u = new User();
        u.email = email.toLowerCase().strip();
        u.passwordHash = passwordHash;
        u.username = username.strip();
        u.role = UserRole.USER;
        u.status = AccountStatus.ACTIVE;
        u.profile = UserProfile.empty();
        u.createdAt = Instant.now();
        return u;
    }

    public static User reconstitute(Long id, String email, String passwordHash,
                                    String username, UserRole role,
                                    AccountStatus status, UserProfile profile,
                                    Instant createdAt) {
        User u = new User();
        u.id = id;
        u.email = email;
        u.passwordHash = passwordHash;
        u.username = username;
        u.role = role;
        u.status = status;
        u.profile = profile;
        u.createdAt = createdAt;
        return u;
    }

    public void updateUsername(String newUsername) {
        validateUsername(newUsername);
        this.username = newUsername.strip();
    }

    public void updateProfile(UserProfile updatedProfile) {
        this.profile = updatedProfile;
    }

    public void updateAvatarId(Long id) {
        this.profile = this.profile.withAvatarId(id);
    }

    public void ban() {
        if (this.status == AccountStatus.BANNED) throw new AlreadyBannedException(this.email);
        if (this.status == AccountStatus.DELETED) throw new AccountDeletedException(this.email);
        this.status = AccountStatus.BANNED;
    }

    public void unban() {
        if (this.status != AccountStatus.BANNED) throw new NotBannedException(this.email);
        this.status = AccountStatus.ACTIVE;
    }

    public void softDelete() {
        if (this.status == AccountStatus.DELETED) throw new AccountDeletedException(this.email);
        this.email = "deleted_" + this.id + "@deleted.fitapp";
        this.passwordHash = "[deleted]";
        this.username = "deleted_" + this.id;
        this.profile = UserProfile.empty();
        this.status = AccountStatus.DELETED;
    }

    public void changeRole(UserRole newRole) {
        if (this.status == AccountStatus.DELETED) throw new AccountDeletedException(this.email);
        this.role = newRole;
    }

    public boolean isActive() {
        return AccountStatus.ACTIVE.equals(this.status);
    }

    public boolean isAdmin() {
        return UserRole.ADMIN.equals(this.role);
    }

    private static void validateEmail(String email) {
        if (email == null || !email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,}$")) {
            throw new InvalidEmailException(email);
        }
    }

    private static void validateUsername(String name) {
        if (name == null || name.isBlank() || name.length() < 3 || name.length() > 30
                || !name.matches("^[a-zA-Z0-9_]+$")) {
        throw new InvalidUsernameException(
                    "Username must be 3-30 characters and contain only letters, numbers and underscores");
        }
    }
}