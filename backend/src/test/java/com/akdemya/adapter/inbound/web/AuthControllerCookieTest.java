package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.infrastructure.security.JwtService;
import com.akdemya.adapter.infrastructure.security.OAuth2CodeStore;
import com.akdemya.domain.port.in.AuthUseCase;
import com.akdemya.domain.port.in.RefreshTokenUseCase;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerCookieTest {

    private final AuthUseCase authUseCase = mock(AuthUseCase.class);
    private final OAuth2CodeStore codeStore = mock(OAuth2CodeStore.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final RefreshTokenUseCase refreshTokenUseCase = mock(RefreshTokenUseCase.class);

    private AuthController controller;

    @BeforeEach
    void setUp() {
        // secure=false to test cookie attributes without HTTPS in unit tests
        controller = new AuthController(authUseCase, codeStore, jwtService, refreshTokenUseCase, false);
    }

    // ── login ──

    @Test
    void login_setsCookieAndReturnsRole() {
        var cmd = new AuthUseCase.LoginCommand("a@b.com", "pass");
        when(authUseCase.login(cmd))
            .thenReturn(AuthUseCase.AuthResponse.success("jwt.token.here", "refresh-token", "STUDENT"));

        MockHttpServletResponse res = new MockHttpServletResponse();
        ResponseEntity<?> response = controller.login(cmd, res);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertFalse(body.containsKey("accessToken"), "accessToken must NOT be in response body");
        assertEquals("STUDENT", body.get("role"));

        Cookie cookie = res.getCookie("ak_token");
        assertNotNull(cookie, "ak_token cookie must be set");
        assertEquals("jwt.token.here", cookie.getValue());
        assertTrue(cookie.isHttpOnly(), "cookie must be httpOnly");
        assertEquals("/api", cookie.getPath());
        assertEquals(900, cookie.getMaxAge());

        // ak_refresh cookie is set as ResponseCookie header (may be a separate Set-Cookie entry)
        String refreshCookieHeader = res.getHeaders("Set-Cookie").stream()
            .filter(h -> h.contains("ak_refresh="))
            .findFirst()
            .orElse(null);
        assertNotNull(refreshCookieHeader, "ak_refresh cookie header must be set");
        assertTrue(refreshCookieHeader.contains("ak_refresh=refresh-token"));
        assertTrue(refreshCookieHeader.contains("Path=/api/auth"));
    }

    @Test
    void login_secureFlagRespectedFromConfig() {
        // With secure=true
        AuthController secureController = new AuthController(authUseCase, codeStore, jwtService, refreshTokenUseCase, true);
        var cmd = new AuthUseCase.LoginCommand("a@b.com", "pass");
        when(authUseCase.login(cmd))
            .thenReturn(AuthUseCase.AuthResponse.success("tok", "refresh-token", "STUDENT"));

        MockHttpServletResponse res = new MockHttpServletResponse();
        secureController.login(cmd, res);

        Cookie cookie = res.getCookie("ak_token");
        assertNotNull(cookie);
        assertTrue(cookie.getSecure(), "secure flag must be set when configured");
    }

    @Test
    void login_insecureFlagRespectedFromConfig() {
        var cmd = new AuthUseCase.LoginCommand("a@b.com", "pass");
        when(authUseCase.login(cmd))
            .thenReturn(AuthUseCase.AuthResponse.success("tok", "refresh-token", "STUDENT"));

        MockHttpServletResponse res = new MockHttpServletResponse();
        controller.login(cmd, res);

        Cookie cookie = res.getCookie("ak_token");
        assertNotNull(cookie);
        assertFalse(cookie.getSecure(), "secure flag must NOT be set when configured as false");
    }

    // ── register ──

    @Test
    void register_setsCookieAndReturnsRole() {
        var cmd = new AuthUseCase.RegisterCommand("a@b.com", "pass", "pass", "A", "B", "dev");
        when(authUseCase.register(cmd))
            .thenReturn(AuthUseCase.AuthResponse.success("jwt.reg.token", "refresh-token", "STUDENT"));

        MockHttpServletResponse res = new MockHttpServletResponse();
        ResponseEntity<?> response = controller.register(cmd, res);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertFalse(body.containsKey("accessToken"), "accessToken must NOT be in response body");
        assertEquals("STUDENT", body.get("role"));

        Cookie cookie = res.getCookie("ak_token");
        assertNotNull(cookie);
        assertEquals("jwt.reg.token", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/api", cookie.getPath());
        assertEquals(900, cookie.getMaxAge());

        String refreshCookieHeader = res.getHeaders("Set-Cookie").stream()
            .filter(h -> h.contains("ak_refresh="))
            .findFirst()
            .orElse(null);
        assertNotNull(refreshCookieHeader, "ak_refresh cookie header must be set");
        assertTrue(refreshCookieHeader.contains("ak_refresh=refresh-token"));
    }

    // ── logout ──

    @Test
    void logout_clearsCookieWithMaxAgeZero() {
        MockHttpServletResponse res = new MockHttpServletResponse();
        ResponseEntity<?> response = controller.logout(null, res);

        assertEquals(204, response.getStatusCodeValue());

        Cookie cookie = res.getCookie("ak_token");
        assertNotNull(cookie, "ak_token cookie must be set on logout");
        assertEquals("", cookie.getValue());
        assertEquals(0, cookie.getMaxAge());
        assertEquals("/api", cookie.getPath());
    }

    @Test
    void logout_revokesRefreshToken_whenPresent() {
        MockHttpServletResponse res = new MockHttpServletResponse();
        controller.logout("some-refresh-token", res);

        verify(refreshTokenUseCase).revoke("some-refresh-token");
    }

    // ── /me ──

    @Test
    void me_withValidToken_returnsEmailAndRole() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        Cookie authCookie = new Cookie("ak_token", "valid.jwt.token");
        req.setCookies(authCookie);

        @SuppressWarnings("unchecked")
        Jws<Claims> jws = mock(Jws.class);
        Claims claims = mock(Claims.class);
        when(jws.getBody()).thenReturn(claims);
        when(claims.getSubject()).thenReturn("user@example.com");
        when(claims.get("role")).thenReturn("STUDENT");
        when(jwtService.parse("valid.jwt.token")).thenReturn(jws);

        ResponseEntity<?> response = controller.me(req);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("user@example.com", body.get("email"));
        assertEquals("STUDENT", body.get("role"));
    }

    @Test
    void me_withNoToken_returns401() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        // No cookies set

        ResponseEntity<?> response = controller.me(req);

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void me_withInvalidToken_returns401() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        Cookie authCookie = new Cookie("ak_token", "invalid.token");
        req.setCookies(authCookie);

        when(jwtService.parse("invalid.token")).thenThrow(new RuntimeException("invalid token"));

        ResponseEntity<?> response = controller.me(req);

        assertEquals(401, response.getStatusCodeValue());
    }

    // ── oauth2 exchange (now sets cookie too) ──

    @Test
    void exchangeValidCode_setsCookieAndReturnsRole() {
        String code = "valid-uuid-code";
        when(codeStore.exchange(code))
            .thenReturn(Optional.of(new OAuth2CodeStore.ExchangeResult("jwt.token.here", "refresh-token", "STUDENT")));

        MockHttpServletResponse res = new MockHttpServletResponse();
        var request = new AuthController.ExchangeCodeRequest(code);
        ResponseEntity<?> response = controller.exchangeOAuth2Code(request, res);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertFalse(body.containsKey("accessToken"), "accessToken must NOT be in response body");
        assertEquals("STUDENT", body.get("role"));

        Cookie cookie = res.getCookie("ak_token");
        assertNotNull(cookie);
        assertEquals("jwt.token.here", cookie.getValue());
    }
}
