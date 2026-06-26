package com.evoting.evotingsystem.security;

import com.evoting.evotingsystem.util.SessionKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object username = session.getAttribute(SessionKeys.AUTH_USERNAME);
                Object role = session.getAttribute(SessionKeys.AUTH_USER_ROLE);
                if (username != null && role != null) {
                    String roleName = role.toString().startsWith("ROLE_")
                            ? role.toString()
                            : "ROLE_" + role;
                    
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleName);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            username.toString(),
                            null,
                            List.of(authority)
                    );
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    // Temporary debug logging
                    System.out.println("[Security Debug] User Authenticated: " + username);
                    System.out.println("[Security Debug] Assigned Authorities: " + authToken.getAuthorities());
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
