package com.holodos.common.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CSRF protection without tokens: the session cookie is sent with cross-site form posts (older Safari ignores
 * SameSite), so state-changing API requests carrying an {@code Origin} of another site — including another
 * port on the same host — are rejected. The web UIs are served from the same origin and are unaffected.
 * Requests without an Origin header (same-origin requests from old browsers, curl) are allowed.
 */
class SameOriginFilter extends OncePerRequestFilter {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final Set<String> trustedOrigins;

    SameOriginFilter(Set<String> trustedOrigins) {
        this.trustedOrigins = trustedOrigins;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SAFE_METHODS.contains(request.getMethod()) || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.equals(ownOrigin(request)) || trustedOrigins.contains(origin)) {
            chain.doFilter(request, response);
            return;
        }
        response.sendError(HttpStatus.FORBIDDEN.value(), "Cross-origin request rejected");
    }

    /** Origin of this server as the browser sees it (proxy headers are applied by forward-headers-strategy). */
    static String ownOrigin(HttpServletRequest request) {
        String scheme = request.getScheme();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        return scheme + "://" + request.getServerName() + (defaultPort ? "" : ":" + port);
    }
}
