package io.github.danielnguyen327.ledgersync.user;

import io.github.danielnguyen327.ledgersync.auth.SignedInUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
class UserController {

    private final AppUserRepository users;

    UserController(AppUserRepository users) {
        this.users = users;
    }

    /** The signed-in user. Every query starts from the id in the session, never from the request. */
    @GetMapping("/api/me")
    UserResponse me(@AuthenticationPrincipal SignedInUser signedIn) {
        return users.findById(signedIn.id())
            .map(UserResponse::from)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)); // account deleted
    }
}