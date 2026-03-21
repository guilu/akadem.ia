package com.akdemya.adapter.infrastructure.security;

import com.akdemya.domain.port.in.AuthUseCase;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class GoogleOAuth2UserService extends DefaultOAuth2UserService {

    private final AuthUseCase authUseCase;

    public GoogleOAuth2UserService(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        authUseCase.loginWithOAuth2(email, name);
        return oAuth2User;
    }
}
