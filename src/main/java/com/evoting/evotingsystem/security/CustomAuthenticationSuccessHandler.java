package com.evoting.evotingsystem.security;

import com.evoting.evotingsystem.entity.UserRole;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationSuccessHandler {

    public String resolveRedirectPath(UserRole role) {
        if (role == UserRole.ADMIN) {
            return "/admin/dashboard";
        }
        return "/voter/dashboard";
    }
}

