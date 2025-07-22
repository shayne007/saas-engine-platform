package com.feng.login;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TODO
 *
 * @since 2025/6/23
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class UserLoginController {

    @Autowired
    private UserLoginService userLoginService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            LoginResponse response = userLoginService.authenticate(
                request.getUsername(),
                request.getPassword(),
                ipAddress,
                userAgent
            );

            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new LoginResponse(false, e.getMessage()));
        }
    }

    private String getClientIpAddress(HttpServletRequest httpRequest) {
        return httpRequest.getHeader("X-FORWARDED-FOR");
    }

    @PostMapping("/logout")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        userLoginService.logout(token);
        return ResponseEntity.ok().build();
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        return request.getHeader("Authorization");
    }
}

