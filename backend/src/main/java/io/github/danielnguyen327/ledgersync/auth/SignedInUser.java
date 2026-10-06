package io.github.danielnguyen327.ledgersync.auth;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Who is signed in. It is saved in the session, so after sign-in it holds only the user's id and
 * email: Spring calls eraseCredentials() before the session is stored.
 */
public final class SignedInUser implements UserDetails, CredentialsContainer {

  @Serial
  private static final long serialVersionUID = 1L;

  private final UUID id;
  private final String email;
  private @Nullable String passwordHash;

  SignedInUser(UUID id, String email, String passwordHash) {
    this.id = id;
    this.email = email;
    this.passwordHash = passwordHash;
  } 
  
  public UUID id() {
    return id;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override 
  public @Nullable String getPassword() {
    return passwordHash;
  }

  @Override 
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override 
  public void eraseCredentials() {
    passwordHash = null;
  }
}