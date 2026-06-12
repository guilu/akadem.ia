package com.akdemya.adapter.infrastructure.security;

import com.akdemya.domain.port.in.AuthUseCase;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthUseCase authUseCase;
    private final OAuth2CodeStore codeStore;
    private final String frontendUrl;

    public OAuth2SuccessHandler(AuthUseCase authUseCase,
                                OAuth2CodeStore codeStore,
                                @Value("${app.frontend-url}") String frontendUrl) {
        this.authUseCase = authUseCase;
        this.codeStore = codeStore;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = token.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");
        AuthUseCase.AuthResponse authResponse;
        try {
            authResponse = authUseCase.loginWithOAuth2(email, name);
        } catch (DataIntegrityViolationException e) {
            // Two concurrent first logins for the same account can both pass
            // the findByEmail check; the loser hits uq_users_email at commit.
            // The user exists now, so one retry takes the existing-user path.
            authResponse = authUseCase.loginWithOAuth2(email, name);
        }
        String code = codeStore.store(authResponse.accessToken(), authResponse.refreshToken(), authResponse.role());
        response.sendRedirect(frontendUrl + "/oauth2/callback?code=" + code);
    }
}
