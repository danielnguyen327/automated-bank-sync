package io.github.danielnguyen327.ledgersync.auth;

import io.github.danielnguyen327.ledgersync.user.AppUser;
import io.github.danielnguyen327.ledgersync.user.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Looks up an account by email when someone signs in with a password. */
@Service 
class SignedInUserService implements UserDetailsService {
  
  private final AppUserRepository users;

  SignedInUserService(AppUserRepository users) {
    this.users = users;
  }

  @Override 
  public UserDetails loadUserByUsername(String email) {
    return users.findByEmail(AppUser.normalizeEmail(email))
        .filter(user -> user.getPasswordHash() != null)
        .map(user -> new SignedInUser(user.getId(), user.getEmail(), user.getPasswordHash()))
        .orElseThrow(() -> new UsernameNotFoundException("No password sign-in for this email"));
  }
}