package io.github.danielnguyen327.ledgersync.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@Configuration 
class SecurityConfig {

  @Bean 
  SecurityFilterChain securityFilterChain(HttpSecurity http, CsrfTokenRepository csrfTokens) throws Exception {
    return http
        .authorizeHttpRequests(requests -> requests
          .requestMatchers("/api/health", "/api/auth/**", "/error").permitAll()
          .anyRequest().authenticated())
        // Every response carries an XSRF-TOKEN cookie; the frontend copies it into an
        // X-XSRF-TOKEN header on every POST. Another site can't read the cookie, so it can't
        // forge that header.
        .csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokens))
        // This is a JSON API: answer 401 instead of redirecting to a login page, and don't open
        // a session just to remember which page someone wanted.
        .exceptionHandling(exceptions -> exceptions
          .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        )
        .requestCache(RequestCacheConfigurer::disable)
        .logout(logout -> logout
          .logoutUrl("/api/auth/logout")
          .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
        )
        .build();
  }

  @Bean 
  CsrfTokenRepository csrfTokenRepository() {
    return CookieCsrfTokenRepository.withHttpOnlyFalse();
    // The frontend has to read it
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    // BCrypt, saved as {bcrypt}$2a$10$...
  }

  @Bean 
  AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider passwordSignIn = new DaoAuthenticationProvider(users);
    passwordSignIn.setPasswordEncoder(passwordEncoder);
    return new ProviderManager(passwordSignIn);
  }
}