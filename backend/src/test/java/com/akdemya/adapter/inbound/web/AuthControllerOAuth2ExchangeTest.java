package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.infrastructure.security.JwtService;
import com.akdemya.adapter.infrastructure.security.OAuth2CodeStore;
import com.akdemya.domain.port.in.AuthUseCase;
import com.akdemya.domain.port.in.RefreshTokenUseCase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerOAuth2ExchangeTest {

    private final AuthUseCase authUseCase = mock(AuthUseCase.class);
    private final OAuth2CodeStore codeStore = mock(OAuth2CodeStore.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final RefreshTokenUseCase refreshTokenUseCase = mock(RefreshTokenUseCase.class);
    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authUseCase, codeStore, jwtService, refreshTokenUseCase, false);
    }

    // --- register ---

    @Test
    void registerSuccess_setsCookieAndReturnsRole() {
        var cmd = new AuthUseCase.RegisterCommand("a@b.com", "pass", "pass", "A", "B", "dev");
        when(authUseCase.register(cmd))
            .thenReturn(AuthUseCase.AuthResponse.success("tok", "refresh-token", "STUDENT"));

        MockHttpServletResponse res = new MockHttpServletResponse();
        ResponseEntity<?> resp = controller.register(cmd, res);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertNotNull(body);
        assertFalse(body.containsKey("accessToken"), "accessToken must NOT be in body");
        assertEquals("STUDENT", body.get("role"));

        Cookie cookie = res.getCookie("ak_token");
        assertNotNull(cookie);
        assertEquals("tok", cookie.getValue());

        String refreshCookieHeader = res.getHeaders("Set-Cookie").stream()
            .filter(h -> h.contains("ak_refresh="))
            .findFirst()
            .orElse(null);
        assertNotNull(refreshCookieHeader);
        assertTrue(refreshCookieHeader.contains("ak_refresh=refresh-token"));
    }

    @Test
    void registerFailure_returns400WithErrorField() {
        var cmd = new AuthUseCase.RegisterCommand("a@b.com", "short", "short", "A", "B", "dev");
        when(authUseCase.register(cmd))
            .thenReturn(AuthUseCase.AuthResponse.fail("password_too_short"));

        MockHttpServletResponse res = new MockHttpServletResponse();
        ResponseEntity<?> resp = controller.register(cmd, res);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertNotNull(body);
        assertEquals("password_too_short", body.get("error"));
    }

    // --- login ---

    @Test
    void loginSuccess_setsCookieAndReturnsRole() {
        var cmd = new AuthUseCase.LoginCommand("a@b.com", "pass");
        when(authUseCase.login(cmd))
            .thenReturn(AuthUseCase.AuthResponse.success("tok", "refresh-token", "ADMIN"));

        MockHttpServletResponse res = new MockHttpServletResponse();
        ResponseEntity<?> resp = controller.login(cmd, res);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertNotNull(body);
        assertFalse(body.containsKey("accessToken"), "accessToken must NOT be in body");
        assertEquals("ADMIN", body.get("role"));

        Cookie cookie = res.getCookie("ak_token");
        assertNotNull(cookie);
        assertEquals("tok", cookie.getValue());

        String refreshCookieHeader = res.getHeaders("Set-Cookie").stream()
            .filter(h -> h.contains("ak_refresh="))
            .findFirst()
            .orElse(null);
        assertNotNull(refreshCookieHeader);
        assertTrue(refreshCookieHeader.contains("ak_refresh=refresh-token"));
    }

    @Test
    void loginFailure_returns401WithErrorField() {
        var cmd = new AuthUseCase.LoginCommand("a@b.com", "wrong");
        when(authUseCase.login(cmd))
            .thenReturn(AuthUseCase.AuthResponse.fail("invalid_credentials"));

        MockHttpServletResponse res = new MockHttpServletResponse();
        ResponseEntity<?> resp = controller.login(cmd, res);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertNotNull(body);
        assertEquals("invalid_credentials", body.get("error"));
    }

    @Test
    void exchangeValidCode_setsCookieAndReturnsRole() {
        String code = "valid-uuid-code";
        when(codeStore.exchange(code))
            .thenReturn(Optional.of(new OAuth2CodeStore.ExchangeResult("jwt.token.here", "refresh-token", "STUDENT")));

        MockHttpServletResponse res = new MockHttpServletResponse();
        var request = new AuthController.ExchangeCodeRequest(code);
        ResponseEntity<?> response = controller.exchangeOAuth2Code(request, res);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertFalse(body.containsKey("accessToken"), "accessToken must NOT be in body");
        assertEquals("STUDENT", body.get("role"));

        Cookie cookie = res.getCookie("ak_token");
        assertNotNull(cookie);
        assertEquals("jwt.token.here", cookie.getValue());

        String refreshCookieHeader = res.getHeaders("Set-Cookie").stream()
            .filter(h -> h.contains("ak_refresh="))
            .findFirst()
            .orElse(null);
        assertNotNull(refreshCookieHeader);
        assertTrue(refreshCookieHeader.contains("ak_refresh=refresh-token"));
    }

    @Test
    void exchangeUnknownCodeReturns400WithErrorField() {
        String code = "unknown-code";
        when(codeStore.exchange(code)).thenReturn(Optional.empty());

        MockHttpServletResponse res = new MockHttpServletResponse();
        var request = new AuthController.ExchangeCodeRequest(code);
        ResponseEntity<?> response = controller.exchangeOAuth2Code(request, res);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("invalid_code", body.get("error"));
    }
}
