package io.github.joicepassos.billing.accounts.internal;

import io.github.joicepassos.billing.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** 401 as RFC 9457 Problem Details. Runs in the filter chain, before controller advice can help. */
class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        Object requestId = request.getAttribute(RequestIdFilter.ATTRIBUTE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/problem+json");
        response.setHeader("WWW-Authenticate", "Bearer");
        response.getWriter().write("{\"type\":\"https://usage-billing-engine/errors/unauthorized\","
                + "\"title\":\"Unauthorized\",\"status\":401,"
                + "\"detail\":\"Send a valid API key as 'Authorization: Bearer <key>'.\""
                + (requestId != null ? ",\"requestId\":\"" + requestId + "\"" : "")
                + "}");
    }
}
