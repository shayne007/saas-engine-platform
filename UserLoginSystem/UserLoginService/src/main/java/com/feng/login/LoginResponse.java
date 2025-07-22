package com.feng.login;

/**
 * TODO
 *
 * @since 2025/6/23
 */
public class LoginResponse {
    boolean success;
    String message;
    public LoginResponse(boolean b, String message) {
        this.success = b;
        this.message = message;
    }
}
