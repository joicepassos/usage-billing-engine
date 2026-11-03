package io.github.joicepassos.billing.accounts.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * Stateless API key security. {@code /v1/**} requires a tenant key; health and API docs are public; everything
 * else is denied. CSRF is off because no cookies or sessions are used.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, AccountsService accounts) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new ApiKeyAuthenticationFilter(accounts), AnonymousAuthenticationFilter.class)
                .exceptionHandling(errors -> errors.authenticationEntryPoint(new ProblemAuthenticationEntryPoint()))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
                        .requestMatchers("/v1/**").authenticated()
                        .anyRequest().denyAll())
                .build();
    }
}
