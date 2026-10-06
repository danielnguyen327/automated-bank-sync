package io.github.danielnguyen327.ledgersync.user;

import java.util.UUID;

/** What the API says about a user. Never includes the password hash. */
public record UserResponse(UUID id, String email, String name) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName());
    }
}