package io.github.joicepassos.billing.accounts.internal;

import io.github.joicepassos.billing.shared.tenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates {@code Authorization: Bearer sk_test_...} and runs the rest of the request as that tenant.
 * Requests without a valid key continue unauthenticated and are rejected by the authorization rules.
 */
class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final AccountsService accounts;

    ApiKeyAuthenticationFilter(AccountsService accounts) {
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<AuthenticatedTenant> tenant = bearerToken(request).flatMap(accounts::authenticate);
        if (tenant.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        AuthenticatedTenant principal = tenant.get();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
        try {
            TenantContext.callAs(principal.tenantId(), () -> {
                chain.doFilter(request, response);
                return null;
            });
        } catch (IOException | ServletException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static Optional<String> bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            return Optional.empty();
        }
        return Optional.of(header.substring(BEARER.length()).strip());
    }
}
