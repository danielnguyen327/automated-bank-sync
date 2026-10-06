package io.github.danielnguyen327.ledgersync.user;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "app_user")
public class AppUser {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private String email;

  private String name;

  private @Nullable String passwordHash;  // null for Google/GitHub accounts

  protected AppUser() {
    // for JPA
  }

  public AppUser(String email, String name, String passwordHash) {
    this.email = normalizeEmail(email);
    this.name = name;
    this.passwordHash = passwordHash;
  }

  /** Emails are compared lowercase, so User@EXAMPLE.COM and user@example.com are the same */
  public static String normalizeEmail(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getName() {
    return name;
  }

  public @Nullable String getPasswordHash() {
    return passwordHash;
  }
}
