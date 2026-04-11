package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.infrastructure.security.JwtService;
import com.akdemya.adapter.infrastructure.security.OAuth2CodeStore;
import com.akdemya.domain.port.in.AuthUseCase;
import com.akdemya.domain.port.in.RefreshTokenUseCase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerRefreshTest {

    private final AuthUseCase authUseCase = mock(AuthUseCase.class);
    private final OAuth2CodeStore codeStore = mock(OAuth2CodeStore.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final RefreshTokenUseCase refreshTokenUseCase = mock(RefreshTokenUseCase.class);
    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authUseCase, codeStore, jwtService, refreshTokenUseCase, false);
    }

    @Test
    void refresh_setsNewCookie_andReturnsRole_onSuccess() {
        String rawToken = "valid-refresh-token";
        when(refreshTokenUseCase.refresh(rawToken))
            .thenReturn(RefreshTokenUseCase.RefreshResult.success("new-access-token", "STUDENT"));

        MockHttpServletResponse res = new MockHttpServletResponse();
        ResponseEntity<?> response = controller.refresh(rawToken, res);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("STUDENT", body.get("role"));

        Cookie cookie = res.getCookie("ak_token");
        assertNotNull(cookie, "ak_token cookie must be set after refresh");
        assertEquals("new-access-token", cookie.getValue());
        assertEquals(900, cookie.getMaxAge());
    }

    @Test
    void refresh_returns401_whenNoCookie() {
        MockHttpServletResponse res = new MockHttpServletResponse();
        ResponseEntity<?> response = controller.refresh(null, res);

        assertEquals(401, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("missing_refresh_token", body.get("error"));
    }

    @Test
    void refresh_returns401_whenTokenInvalid() {
        String rawToken = "invalid-refresh-token";
        when(refreshTokenUseCase.refresh(rawToken))
            .thenReturn(RefreshTokenUseCase.RefreshResult.failure("invalid_refresh_token"));

        MockHttpServletResponse res = new MockHttpServletResponse();
        ResponseEntity<?> response = controller.refresh(rawToken, res);

        assertEquals(401, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("invalid_refresh_token", body.get("error"));
    }

    @Test
    void logout_revokesRefreshToken_andClearsCookies() {
        String rawToken = "refresh-token-to-revoke";

        MockHttpServletResponse res = new MockHttpServletResponse();
        ResponseEntity<?> response = controller.logout(rawToken, res);

        assertEquals(204, response.getStatusCodeValue());
        verify(refreshTokenUseCase).revoke(rawToken);

        Cookie akTokenCookie = res.getCookie("ak_token");
        assertNotNull(akTokenCookie);
        assertEquals(0, akTokenCookie.getMaxAge());

        // ak_refresh cleared via Set-Cookie header
        String refreshClearHeader = res.getHeaders("Set-Cookie").stream()
            .filter(h -> h.contains("ak_refresh="))
            .findFirst()
            .orElse(null);
        assertNotNull(refreshClearHeader);
        assertTrue(refreshClearHeader.contains("Max-Age=0"));
    }
}
