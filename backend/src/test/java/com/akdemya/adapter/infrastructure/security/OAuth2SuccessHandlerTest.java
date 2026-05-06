package com.akdemya.adapter.infrastructure.security;

import com.akdemya.domain.port.in.AuthUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;

import static org.mockito.Mockito.*;

class OAuth2SuccessHandlerTest {

    private AuthUseCase authUseCase;
    private OAuth2CodeStore codeStore;
    private OAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        authUseCase = mock(AuthUseCase.class);
        codeStore = mock(OAuth2CodeStore.class);
        handler = new OAuth2SuccessHandler(authUseCase, codeStore, "http://localhost:5173");
    }

    @Test
    void onAuthenticationSuccess_storesCodeAndRedirects() throws IOException {
        // Arrange
        OAuth2User oAuth2User = mock(OAuth2User.class);
        when(oAuth2User.getAttribute("email")).thenReturn("user@example.com");
        when(oAuth2User.getAttribute("name")).thenReturn("Test User");

        OAuth2AuthenticationToken token = mock(OAuth2AuthenticationToken.class);
        when(token.getPrincipal()).thenReturn(oAuth2User);

        AuthUseCase.AuthResponse authResponse = AuthUseCase.AuthResponse.success("jwt.token.here", "refresh-token", "STUDENT");
        when(authUseCase.loginWithOAuth2("user@example.com", "Test User")).thenReturn(authResponse);
        when(codeStore.store("jwt.token.here", "refresh-token", "STUDENT")).thenReturn("ephemeral-code-123");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        handler.onAuthenticationSuccess(request, response, token);

        // Assert
        verify(authUseCase).loginWithOAuth2("user@example.com", "Test User");
        verify(codeStore).store("jwt.token.here", "refresh-token", "STUDENT");
        verify(response).sendRedirect("http://localhost:5173/oauth2/callback?code=ephemeral-code-123");
    }
}
