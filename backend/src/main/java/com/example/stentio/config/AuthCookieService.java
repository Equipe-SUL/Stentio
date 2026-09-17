package com.example.stentio.config;

import com.example.stentio.service.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieService {

    public static final String COOKIE_NAME = "stentio_token";

    private final boolean secure;

    public AuthCookieService(@Value("${app.cookie.secure:false}") boolean secure) {
        this.secure = secure;
    }

    public ResponseCookie criar(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(TokenService.EXPIRES_IN)
                .build();
    }

    public ResponseCookie limpar() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }
}