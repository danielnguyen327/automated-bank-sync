package io.github.danielnguyen327.ledgersync.user;

import io.github.danielnguyen327.ledgersync.auth.SignedInUser;
import io.github.danielnguyen327.ledgersync.bank.BankService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
class UserController {

    private final AppUserRepository users;
    private final BankService banks;
    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    UserController(AppUserRepository users, BankService banks,
            FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.users = users;
        this.banks = banks;
        this.sessions = sessions;
    }

    /** The signed-in user. Every query starts from the id in the session, never from the request. */
    @GetMapping("/api/me")
    UserResponse me(@AuthenticationPrincipal SignedInUser signedIn) {
        return users.findById(signedIn.id())
            .map(UserResponse::from)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)); // account deleted
    }

    /** Deletes the account: every bank is disconnected at Plaid, all data is deleted, and every device signs out. */
    @DeleteMapping("/api/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteMe(@AuthenticationPrincipal SignedInUser signedIn, HttpServletRequest request) {
        banks.disconnectAll(signedIn.id());
        users.deleteById(signedIn.id()); // "on delete cascade" removes the rest of their data
        HttpSession current = request.getSession(false);
        if (current != null) {
            current.invalidate(); // this browser: its session row is deleted and its cookie expires
        }
        sessions.findByPrincipalName(signedIn.getUsername()).keySet().forEach(sessions::deleteById); // other devices
    }
}
