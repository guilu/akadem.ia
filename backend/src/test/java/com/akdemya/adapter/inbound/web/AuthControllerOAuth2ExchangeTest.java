package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.infrastructure.security.OAuth2CodeStore;
import com.akdemya.domain.port.in.AuthUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerOAuth2ExchangeTest {

    private final AuthUseCase authUseCase = mock(AuthUseCase.class);
    private final OAuth2CodeStore codeStore = mock(OAuth2CodeStore.class);
    private final AuthController controller = new AuthController(authUseCase, codeStore);

    // --- register ---

    @Test
    void registerSuccess_returns200WithTokenAndRole() {
        var cmd = new AuthUseCase.RegisterCommand("a@b.com", "pass", "pass", "A", "B", "dev");
        when(authUseCase.register(cmd))
            .thenReturn(AuthUseCase.AuthResponse.success("tok", "STUDENT"));

        ResponseEntity<?> resp = controller.register(cmd);

        assertEquals(200, resp.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertNotNull(body);
        assertEquals("tok", body.get("accessToken"));
        assertEquals("STUDENT", body.get("role"));
    }

    @Test
    void registerFailure_returns400WithErrorField() {
        var cmd = new AuthUseCase.RegisterCommand("a@b.com", "short", "short", "A", "B", "dev");
        when(authUseCase.register(cmd))
            .thenReturn(AuthUseCase.AuthResponse.fail("password_too_short"));

        ResponseEntity<?> resp = controller.register(cmd);

        assertEquals(400, resp.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertNotNull(body);
        assertEquals("password_too_short", body.get("error"));
    }

    // --- login ---

    @Test
    void loginSuccess_returns200WithTokenAndRole() {
        var cmd = new AuthUseCase.LoginCommand("a@b.com", "pass");
        when(authUseCase.login(cmd))
            .thenReturn(AuthUseCase.AuthResponse.success("tok", "ADMIN"));

        ResponseEntity<?> resp = controller.login(cmd);

        assertEquals(200, resp.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertNotNull(body);
        assertEquals("tok", body.get("accessToken"));
        assertEquals("ADMIN", body.get("role"));
    }

    @Test
    void loginFailure_returns401WithErrorField() {
        var cmd = new AuthUseCase.LoginCommand("a@b.com", "wrong");
        when(authUseCase.login(cmd))
            .thenReturn(AuthUseCase.AuthResponse.fail("invalid_credentials"));

        ResponseEntity<?> resp = controller.login(cmd);

        assertEquals(401, resp.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertNotNull(body);
        assertEquals("invalid_credentials", body.get("error"));
    }

    @Test
    void exchangeValidCodeReturns200WithAccessTokenAndRole() {
        String code = "valid-uuid-code";
        when(codeStore.exchange(code))
            .thenReturn(Optional.of(new OAuth2CodeStore.ExchangeResult("jwt.token.here", "STUDENT")));

        var request = new AuthController.ExchangeCodeRequest(code);
        ResponseEntity<?> response = controller.exchangeOAuth2Code(request);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("jwt.token.here", body.get("accessToken"));
        assertEquals("STUDENT", body.get("role"));
    }

    @Test
    void exchangeUnknownCodeReturns400WithErrorField() {
        String code = "unknown-code";
        when(codeStore.exchange(code)).thenReturn(Optional.empty());

        var request = new AuthController.ExchangeCodeRequest(code);
        ResponseEntity<?> response = controller.exchangeOAuth2Code(request);

        assertEquals(400, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("invalid_code", body.get("error"));
    }
}
