package com.evoting.evotingsystem.security;

import com.evoting.evotingsystem.entity.UserRole;
import com.evoting.evotingsystem.util.SessionKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class VoterAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect("/login");
            return false;
        }

        Object userId = session.getAttribute(SessionKeys.AUTH_USER_ID);
        Object role = session.getAttribute(SessionKeys.AUTH_USER_ROLE);
        if (userId == null || role == null) {
            response.sendRedirect("/login");
            return false;
        }

        String roleName = role.toString().startsWith("ROLE_")
                ? role.toString().substring("ROLE_".length())
                : role.toString();
        boolean studentLike = UserRole.VOTER.name().equals(roleName) || UserRole.STUDENT.name().equals(roleName);
        if (!studentLike) {
            response.sendRedirect("/login");
            return false;
        }

        return true;
    }
}
