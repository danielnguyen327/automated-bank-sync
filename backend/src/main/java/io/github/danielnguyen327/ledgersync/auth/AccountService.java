package io.github.danielnguyen327.ledgersync.auth;

import io.github.danielnguyen327.ledgersync.user.AppUser;
import io.github.danielnguyen327.ledgersync.user.AppUserRepository;
import java.nio.charset.StandardCharsets;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service 
class AccountService {

  private static final String EMAIL_TAKEN = "An account with this email already exists.";

  private final AppUserRepository users;
  private final PasswordEncoder passwordEncode;

  AccountService(AppUserRepository users, PasswordEncoder passwordEncode) {
    this.users = users;
    this.passwordEncode = passwordEncode;
  }

  /** Creates an account that signs in with email and password. */
  AppUser register(String name, String email, String password) {
    // BCrypt reads at most 72 bytes, and Spring refuses to hash anything longer.
    if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a shorter password");
    }
    String normalizedEmail = AppUser.normalizeEmail(email);
    if (users.existsByEmail(normalizedEmail)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, EMAIL_TAKEN);
    }
    try {
      return users.save(new AppUser(normalizedEmail, name.strip(), passwordEncode.encode(password)));
    } catch (DataIntegrityViolationException e) {
      // Two sign-ups with the same email at the same moment: the unique index stops the second.
      throw new ResponseStatusException(HttpStatus.CONFLICT, EMAIL_TAKEN);
    }
  }
}