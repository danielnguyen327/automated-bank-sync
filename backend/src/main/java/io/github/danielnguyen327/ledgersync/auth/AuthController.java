package io.github.danielnguyen327.ledgersync.auth;

import io.github.danielnguyen327.ledgersync.user.AppUserRepository;
import io.github.danielnguyen327.ledgersync.user.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController 
@RequestMapping("/api/auth")
class AuthController {
  
  record RegisterRequest(
    @NotBlank(message = "Enter your name.")
    @Size(max = 100, message = "Use a name of 100 characters or fewer.")
    String name,
    @NotBlank(message = "Enter your email.")
    @Email(message = "Enter a valid email.")
    @Size(max = 254, message = "Use an email of 254 characters or fewer.")
    String email,
    @NotNull(message = "Choose a password.")
    @Size(min = 12, max = 72, message = "Use a password of 12 to 72 characters.")
    String password) {
    }

    record LoginRequest(
      @NotBlank(message = "Enter your email.") String email,
      @NotBlank(message = "Enter your password.") String password) {
    }

    private final AccountService accounts;
    private final AppUserRepository users;
    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionStrategy;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    AuthController(AccountService accounts, AppUserRepository users, AuthenticationManager autheticationManager, CsrfTokenRepository csrfTokens) {
      this.accounts = accounts;
      this.users = users;
      this.authenticationManager = autheticationManager;
      // What Spring's own login form does: a new session id, so a session id planted before
      // sign-in is useless afterwards, and a new CSRF token.
      this.sessionStrategy = new CompositeSessionAuthenticationStrategy(List.of(new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrfTokens)));
    }

    /** Spring adds the XSRF-TOKEN cookie to any response that lacks it; this is a cheap way to get one. */
    @GetMapping("/csrf")
    ResponseEntity<Void> csrf() {
      return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody RegisterRequest body, HttpServletRequest request, HttpServletResponse response) {
      accounts.register(body.name(), body.email(), body.password());
      return signIn(body.email(), body.password(), request, response);
    }

    @PostMapping("/login")
    UserResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
      return signIn(body.email(), body.password(), request, response);
    }

    /** Checks the password, then saves who signed in to the session (a row in spring_session). */
    private UserResponse signIn(String email, String password, HttpServletRequest request, HttpServletResponse response) {
      Authentication authentication;
      try {
        authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password));
      } catch (AuthenticationException e) {
        // Same answer for an unknown email and a wrong password, so nobody can probe for accounts.
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
      }
      sessionStrategy.onAuthentication(authentication, request, response);
      SecurityContext context = securityContextHolderStrategy.createEmptyContext();
      context.setAuthentication(authentication);
      securityContextHolderStrategy.setContext(context);
      securityContextRepository.saveContext(context, request, response);

      SignedInUser signedIn = (SignedInUser) authentication.getPrincipal();
      return users.findById(signedIn.id()).map(UserResponse::from).orElseThrow();
    }
}