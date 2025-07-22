package com.feng.permission;

import java.io.IOException;
import java.util.Collection;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * TODO
 *
 * @since 2025/3/11
 */
@Component
public class UrlPermissionFilter extends OncePerRequestFilter {
private final UrlAccessPermissionService urlAccessPermissionService;

    public UrlPermissionFilter(UrlAccessPermissionService urlAccessPermissionService) {
        this.urlAccessPermissionService = urlAccessPermissionService;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
        FilterChain filterChain) throws ServletException, IOException {
        String requestUrl = httpServletRequest.getRequestURI();
        String method = httpServletRequest.getMethod();

        // Check if the user has permission to access the URL
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assert authentication != null;
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        // Check if the user has permission to access the URL
        if (urlAccessPermissionService.hasPermission(requestUrl, method,authorities)) {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } else {
            httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "No permission to access this URL");
        }

    }
}
