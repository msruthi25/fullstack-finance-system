package com.example.demo.Security;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

// Runs on every request before it hits the controller
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = null;

        // 1. Try to get token from HttpOnly cookie (preferred — browser sends
        // automatically)
        if (request.getCookies() != null) {
            jwt = Arrays.stream(request.getCookies())
                    .filter(c -> "auth_token".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        // 2. Fallback: read from Authorization header (useful for Postman / API
        // clients)
        if (jwt == null) {
            final String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
            }
        }

        // 3. No token found → let Spring Security decide (will block protected routes)
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Extract email and authenticate if not already done.
        // Wrap in try/catch so an expired or malformed token on a public endpoint
        // (e.g. /user/login called with a stale cookie) doesn't crash with a 500.
        try {
            final String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // Token is expired — continue unauthenticated.
            // Public routes (login/signup) will still work via permitAll().
            // Protected routes will be blocked by Spring Security as expected.
            logger.warn("JWT expired for request [" + request.getRequestURI() + "] — continuing unauthenticated");
        } catch (io.jsonwebtoken.JwtException e) {
            // Token is malformed or has an invalid signature — treat as unauthenticated
            logger.warn("Invalid JWT for request [" + request.getRequestURI() + "]: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
