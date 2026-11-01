package dev.saisrin.cage;

import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;

@org.springframework.context.annotation.Configuration
class Security {
  static final Map<String, List<String>> PERMISSIONS =
      Map.of(
          "CASHIER", List.of("PATRON", "TRANSACTION"),
          "SUPERVISOR", List.of("PATRON", "TRANSACTION", "SUPERVISE"),
          "ANALYST", List.of("PATRON", "TRANSACTION_READ", "CTR", "SAR"),
          "APPROVER", List.of("PATRON", "TRANSACTION_READ", "CTR", "SAR", "APPROVE"),
          "FILER", List.of("PATRON", "TRANSACTION_READ", "CTR", "SAR", "TAX", "FILE"),
          "TAX", List.of("PATRON", "TRANSACTION_READ", "TAX"),
          "ADMIN", List.of("SETUP", "PATRON", "TRANSACTION_READ"),
          "AUDITOR", List.of("PATRON_READ", "TRANSACTION_READ", "CTR_READ", "AUDIT"));

  @Bean
  PasswordEncoder encoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetails(Users users) {
    return name -> {
      CageUser u =
          users
              .findByUsername(name)
              .orElseThrow(() -> new UsernameNotFoundException("Invalid account"));
      Set<String> authorities = new HashSet<>();
      for (String role : u.roles.split(",")) {
        authorities.add("ROLE_" + role);
        authorities.addAll(PERMISSIONS.getOrDefault(role, List.of()));
      }
      return User.withUsername(u.username)
          .password(u.password)
          .authorities(authorities.toArray(String[]::new))
          .disabled(!u.enabled)
          .build();
    };
  }

  @Bean
  SecurityFilterChain filters(HttpSecurity h) throws Exception {
    var csrf = new HttpSessionCsrfTokenRepository();
    csrf.setHeaderName("X-CSRF-TOKEN");
    h.csrf(
            c ->
                c.csrfTokenRepository(csrf)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/api/csrf",
                        "/api/demo",
                        "/actuator/health",
                        "/actuator/health/**",
                        "/",
                        "/index.html",
                        "/assets/**",
                        "/favicon.svg")
                    .permitAll()
                    .requestMatchers(
                        "/api/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .formLogin(
            f ->
                f.loginProcessingUrl("/api/login")
                    .successHandler(
                        (q, s, a) -> {
                          s.setContentType("application/json");
                          s.getWriter().write("{\"ok\":true}");
                        })
                    .failureHandler(
                        (q, s, e) -> {
                          s.setStatus(401);
                          s.setContentType("application/json");
                          s.getWriter().write("{\"message\":\"Incorrect username or password\"}");
                        })
                    .permitAll())
        .logout(l -> l.logoutUrl("/api/logout").logoutSuccessHandler((q, s, a) -> s.setStatus(204)))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (q, s, x) -> {
                          s.setStatus(401);
                          s.setContentType("application/json");
                          s.getWriter().write("{\"message\":\"Please sign in\"}");
                        })
                    .accessDeniedHandler(
                        (q, s, x) -> {
                          s.setStatus(403);
                          s.setContentType("application/json");
                          s.getWriter()
                              .write("{\"message\":\"Access denied or expired security token\"}");
                        }));
    return h.build();
  }
}
